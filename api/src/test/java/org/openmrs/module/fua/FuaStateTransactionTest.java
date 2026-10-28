package org.openmrs.module.fua;

import java.util.UUID;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.api.dao.FuaDao;
import org.openmrs.module.fua.api.dao.FuaVersionDao;
import org.openmrs.module.fua.api.impl.FuaServiceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import static org.junit.Assert.*;

/** Real disposable SQL transactions with the same annotation-driven proxy as the module. */
public class FuaStateTransactionTest {

    private JdbcTemplate database;
    private FuaService service;
    private boolean failAfterWrite;

    @After
    public void discardOwnedDatabase() {
        if (database != null) {
            database.execute("SHUTDOWN");
        }
    }

    @Before
    public void setUp() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:fua_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        source.setDriverClassName("org.h2.Driver");
        database = new JdbcTemplate(source);
        database.execute("CREATE TABLE current_fua (id INT PRIMARY KEY, state INT, revision INT)");
        database.execute("CREATE TABLE previous_fua (fua_id INT, state INT, revision INT)");
        database.update("INSERT INTO current_fua VALUES (1, 10, 3)");
        FuaServiceImpl target = new FuaServiceImpl();
        target.setDao(new FuaDao() {
            @Override
            public Fua getFua(Integer id) {
                return database.queryForObject("SELECT state, revision FROM current_fua WHERE id=?", (row, index) -> {
                    Fua fua = new Fua();
                    fua.setId(id);
                    fua.setVersion(row.getInt("revision"));
                    FuaEstado estado = new FuaEstado();
                    estado.setId(row.getInt("state"));
                    fua.setFuaEstado(estado);
                    return fua;
                }, id);
            }

            @Override
            public Fua saveFua(Fua fua) {
                database.update("UPDATE current_fua SET state=?, revision=? WHERE id=?",
                        fua.getFuaEstado().getId(), fua.getVersion(), fua.getId());
                if (failAfterWrite) {
                    throw new APIException("Synthetic failure after database writes");
                }
                return fua;
            }
        });
        target.setVersionDao(new FuaVersionDao() {
            @Override
            public FuaVersion saveFuaVersion(FuaVersion previous) {
                database.update("INSERT INTO previous_fua VALUES (?, ?, ?)",
                        previous.getFuaId(), previous.getFuaEstado().getId(), previous.getVersion());
                return previous;
            }
        });
        TransactionProxyFactoryBean factory = new TransactionProxyFactoryBean();
        factory.setTarget(target);
        factory.setTransactionManager(new DataSourceTransactionManager(source));
        factory.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        factory.afterPropertiesSet();
        service = (FuaService) factory.getObject();
    }

    @Test
    public void stateAndPreviousVersionCommitTogether() {
        FuaEstado next = new FuaEstado();
        next.setId(11);
        service.updateEstadoFua(1, next);
        assertEquals(Integer.valueOf(11), database.queryForObject("SELECT state FROM current_fua", Integer.class));
        assertEquals(Integer.valueOf(4), database.queryForObject("SELECT revision FROM current_fua", Integer.class));
        assertEquals(Integer.valueOf(1), database.queryForObject("SELECT COUNT(*) FROM previous_fua", Integer.class));
        assertEquals(Integer.valueOf(10), database.queryForObject("SELECT state FROM previous_fua", Integer.class));
        assertEquals(Integer.valueOf(3), database.queryForObject("SELECT revision FROM previous_fua", Integer.class));
    }

    @Test
    public void failureAfterBothWritesRollsBackHistoryAndState() {
        failAfterWrite = true;
        FuaEstado next = new FuaEstado();
        next.setId(11);
        try {
            service.updateEstadoFua(1, next);
            fail("Injected persistence failure must escape the transaction");
        } catch (APIException expected) {
            assertEquals(Integer.valueOf(10), database.queryForObject("SELECT state FROM current_fua", Integer.class));
            assertEquals(Integer.valueOf(3), database.queryForObject("SELECT revision FROM current_fua", Integer.class));
            assertEquals(Integer.valueOf(0), database.queryForObject("SELECT COUNT(*) FROM previous_fua", Integer.class));
        }
    }
}

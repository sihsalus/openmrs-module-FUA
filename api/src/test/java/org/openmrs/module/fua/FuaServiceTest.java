package org.openmrs.module.fua;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.fua.api.dao.FuaDao;
import org.openmrs.module.fua.api.dao.FuaVersionDao;
import org.openmrs.module.fua.api.impl.FuaServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests unitarios para FuaServiceImpl 
 *
 * Se utiliza un "InMemoryFuaDao" que extiende FuaDao y sobreescribe los métodos
 * necesarios para verificar el comportamiento del servicio sin tocar base de datos
 * ni Hibernate.
 */
public class FuaServiceTest {

    private InMemoryFuaDao inMemoryDao;
    private FuaServiceImpl fuaService;

    private FuaVersion savedVersion;

    @Before
    public void setUp() {
        inMemoryDao = new InMemoryFuaDao();
        fuaService = new FuaServiceImpl();
        fuaService.setDao(inMemoryDao); 
        fuaService.setVersionDao(new FuaVersionDao() {
            @Override
            public FuaVersion saveFuaVersion(FuaVersion previous) {
                savedVersion = previous;
                return previous;
            }
        });
    }

    @Test
    public void saveFua_shouldDelegateToDaoAndReturnResult() {
        // given
        Fua fua = new Fua();

        // when
        Fua result = fuaService.saveFua(fua);

        // then
        // el DAO de pruebas guarda el último Fua recibido
        assertSame("El servicio debe delegar en el DAO", fua, inMemoryDao.lastSavedFua);
        assertSame("El servicio debe devolver el mismo Fua que guarda el DAO", fua, result);
    }

    @Test
    public void getFuasFiltrados_shouldCalculateOffsetBasedOnPageAndSize() {
        // given
        String estado = "APROBADO";
        LocalDate inicio = LocalDate.of(2025, 1, 1);
        LocalDate fin = LocalDate.of(2025, 1, 31);
        int page = 2;
        int size = 5;
        int expectedOffset = (page - 1) * size; // 5

        List<Fua> expectedList = Arrays.asList(new Fua(), new Fua());
        inMemoryDao.fuasFiltradosToReturn = expectedList;

        // when
        List<Fua> result = fuaService.getFuasFiltrados(estado, inicio, fin, page, size);

        // then
        assertEquals("Offset calculado incorrectamente", expectedOffset, inMemoryDao.lastOffset);
        assertEquals("Limit incorrecto", size, inMemoryDao.lastLimit);
        assertEquals("Estado pasado al DAO incorrecto", estado, inMemoryDao.lastEstadoNombre);
        assertEquals(inicio, inMemoryDao.lastFechaInicio);
        assertEquals(fin, inMemoryDao.lastFechaFin);
        assertEquals(expectedList, result);
    }

    @Test(expected = APIException.class)
    public void updateEstadoFua_shouldThrowIfFuaNotFound() {
        // given
        Integer fuaId = 123;
        FuaEstado nuevoEstado = new FuaEstado();

        inMemoryDao.fuaToReturn = null; 

        // when
        fuaService.updateEstadoFua(fuaId, nuevoEstado);

        // then -> manejado por expected = APIException.class
    }

    @Test(expected = APIException.class)
    public void updateEstadoFua_shouldThrowIfNuevoEstadoIsNull() {
        // given
        Integer fuaId = 10;
        inMemoryDao.fuaToReturn = new Fua(); 

        // when
        fuaService.updateEstadoFua(fuaId, null);

        // then -> manejado por expected = APIException.class
    }

    @Test
    public void updateEstadoFua_shouldUpdateEstadoAndSaveFua() {
        // given
        Integer fuaId = 1;
        Fua existingFua = new Fua();
        existingFua.setVersion(3);
        FuaEstado previousEstado = new FuaEstado();
        previousEstado.setId(2);
        existingFua.setFuaEstado(previousEstado);
        FuaEstado nuevoEstado = new FuaEstado();
        nuevoEstado.setId(1); // asignamos un ID válido

        inMemoryDao.fuaToReturn = existingFua;

        // when
        Fua result = fuaService.updateEstadoFua(fuaId, nuevoEstado);

        // then
        assertSame("El estado del FUA debe actualizarse", nuevoEstado, existingFua.getFuaEstado());
        assertSame("El FUA actualizado debe haberse guardado en el DAO", existingFua, inMemoryDao.lastSavedFua);
        assertSame(previousEstado, savedVersion.getFuaEstado());
        assertEquals(Integer.valueOf(3), savedVersion.getVersion());
        assertEquals(Integer.valueOf(4), existingFua.getVersion());
        assertSame("El servicio debe devolver el FUA guardado", existingFua, result);
        assertEquals("El servicio debe pedir el FUA al DAO con el ID correcto", fuaId, inMemoryDao.lastFuaId);
    }

    @Test
    public void getAllFuas_shouldReturnListFromDao() {
        List<Fua> listado = Arrays.asList(new Fua(), new Fua());
        inMemoryDao.allFuasToReturn = listado;

        List<Fua> result = fuaService.getAllFuas();

        assertSame("Debe devolver exactamente la lista que da el DAO", listado, result);
    }

    @Test
    public void getFua_shouldDelegateToDao() {
        Integer id = 42;
        Fua fua = new Fua();
        inMemoryDao.fuaToReturn = fua;

        Fua result = fuaService.getFua(id);

        assertSame(fua, result);
        assertEquals("Debe llamar al DAO con el ID correcto", id, inMemoryDao.lastFuaId);
    }

    @Test
    public void getFuaByUuid_shouldDelegateToDao() {
        String uuid = "abc-123";
        Fua fua = new Fua();
        inMemoryDao.fuaByUuidToReturn = fua;

        Fua result = fuaService.getFuaByUuid(uuid);

        assertSame(fua, result);
        assertEquals("UUID pasado al DAO incorrecto", uuid, inMemoryDao.lastUuid);
    }

    @Test
    public void getFuaByVisitUuid_shouldDelegateToDao() {
        String visitUuid = "visit-999";
        Fua fua = new Fua();
        inMemoryDao.fuaByVisitUuidToReturn = fua;

        Fua result = fuaService.getFuaByVisitUuid(visitUuid);

        assertSame(fua, result);
        assertEquals("visitUuid pasado al DAO incorrecto", visitUuid, inMemoryDao.lastVisitUuid);
    }

    @Test
    public void getFuaById_shouldDelegateToDaoGetFua() {
        Integer id = 7;
        Fua fua = new Fua();
        inMemoryDao.fuaToReturn = fua;

        Fua result = fuaService.getFuaById(id);

        assertSame(fua, result);
        assertEquals("Debe reutilizar getFua del DAO", id, inMemoryDao.lastFuaId);
    }

    @Test
    public void purgeFua_shouldDelegateToDao() {
        Fua fua = new Fua();

        fuaService.purgeFua(fua);

        assertSame("Debe delegar purgeFua al DAO", fua, inMemoryDao.lastPurgedFua);
    }


    
    private static class InMemoryFuaDao extends FuaDao {

        // Campos para inspeccionar luego en los asserts
        Fua lastSavedFua;
        Fua lastPurgedFua;
        Integer lastFuaId;
        String lastUuid;
        String lastVisitUuid;
        String lastEstadoNombre;
        LocalDate lastFechaInicio;
        LocalDate lastFechaFin;
        int lastOffset;
        int lastLimit;

        // Valores que el DAO de pruebas devolverá
        List<Fua> allFuasToReturn = new ArrayList<>();
        Fua fuaToReturn;
        Fua fuaByUuidToReturn;
        Fua fuaByVisitUuidToReturn;
        List<Fua> fuasFiltradosToReturn = new ArrayList<>();

        // ---- overrides ----

        @Override
        public List<Fua> getAllFuas() {
            return allFuasToReturn;
        }

        @Override
        public Fua getFua(Integer fuaId) {
            this.lastFuaId = fuaId;
            return fuaToReturn;
        }

        @Override
        public Fua saveFua(Fua fua) {
            this.lastSavedFua = fua;
            return fua;
        }

        @Override
        public void purgeFua(Fua fua) {
            this.lastPurgedFua = fua;
        }

        @Override
        public Fua getFuaByUuid(String uuid) {
            this.lastUuid = uuid;
            return fuaByUuidToReturn;
        }

        @Override
        public Fua getFuaByVisitUuid(String visitUuid) {
            this.lastVisitUuid = visitUuid;
            return fuaByVisitUuidToReturn;
        }

        @Override
        public List<Fua> getFuasFiltrados(String estadoNombre, LocalDate fechaInicio,
                                          LocalDate fechaFin, int offset, int limit) {
            this.lastEstadoNombre = estadoNombre;
            this.lastFechaInicio = fechaInicio;
            this.lastFechaFin = fechaFin;
            this.lastOffset = offset;
            this.lastLimit = limit;
            return fuasFiltradosToReturn;
        }
    }

}

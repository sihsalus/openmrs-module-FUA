package org.openmrs.module.fua;

import org.openmrs.BaseOpenmrsObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "fua_version")
public class FuaVersion extends BaseOpenmrsObject implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "fua_version_id")
	private Integer id;
	
	@Column(name = "uuid", unique = true, nullable = false, length = 38)
	private String uuid;
	
	@Column(name = "fua_id", nullable = false)
	private Integer fuaId;
	
	@Column(name = "fua_uuid", nullable = false, length = 38)
	private String fuaUuid;
	
	@Column(name = "visit_uuid", nullable = false, length = 38)
	private String visitUuid;
	
	@Column(name = "name", nullable = false, length = 255)
	private String name;
	
	@Lob
	@Column(name = "payload")
	private String payload;

	@Column(name = "fua_generator_uuid", length = 38)
	private String fuaGeneratorUuid;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fua_estado_id", nullable = false)
	private FuaEstado fuaEstado;
	
	@Column(name = "descripcion", length = 255)
	private String descripcion;
	
	@Column(name = "fecha_creacion")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaCreacion;
	
	@Column(name = "fecha_actualizacion")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaActualizacion;
	
	@Column(name = "version", nullable = false)
	private Integer version;
	
	@Column(name = "activo", nullable = false)
	private Boolean activo;
	
	public FuaVersion() {
	}
	
	public FuaVersion(Fua fua) {
		this.uuid = UUID.randomUUID().toString();
		this.fuaId = fua.getId();
		this.fuaUuid = fua.getUuid();
		this.visitUuid = fua.getVisitUuid();
		this.name = fua.getName();
		this.payload = fua.getPayload();
		this.fuaEstado = fua.getFuaEstado();
		this.fechaCreacion = fua.getFechaCreacion();
		this.fechaActualizacion = fua.getFechaActualizacion();
		this.version = fua.getVersion();
		this.activo = fua.getActivo();
		this.fuaGeneratorUuid = fua.getFuaGeneratorUuid();
	}
	
	// --- Getters y Setters ---
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Override
	public String getUuid() {
		return uuid;
	}
	
	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public Integer getFuaId() {
		return fuaId;
	}
	
	public void setFuaId(Integer fuaId) {
		this.fuaId = fuaId;
	}
	
	public String getFuaUuid() {
		return fuaUuid;
	}
	
	public void setFuaUuid(String fuaUuid) {
		this.fuaUuid = fuaUuid;
	}
	
	public String getVisitUuid() {
		return visitUuid;
	}
	
	public void setVisitUuid(String visitUuid) {
		this.visitUuid = visitUuid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPayload() {
		return payload;
	}
	
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public FuaEstado getFuaEstado() {
		return fuaEstado;
	}
	
	public void setFuaEstado(FuaEstado fuaEstado) {
		this.fuaEstado = fuaEstado;
	}
	
	public String getDescripcion() {
		return descripcion;
	}
	
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	public Date getFechaCreacion() {
		return fechaCreacion;
	}
	
	public Date getFechaActualizacion() {
		return fechaActualizacion;
	}
	
	public Integer getVersion() {
		return version;
	}
	
	public Boolean getActivo() {
		return activo;
	}

	public String getFuaGeneratorUuid() {
		return fuaGeneratorUuid;
	}

	public void setFuaGeneratorUuid(String fuaGeneratorUuid) {
		this.fuaGeneratorUuid = fuaGeneratorUuid;
	}

}

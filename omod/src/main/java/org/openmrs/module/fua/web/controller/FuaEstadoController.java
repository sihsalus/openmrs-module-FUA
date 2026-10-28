package org.openmrs.module.fua.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.web.utils.FuaAccess;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.fua.FuaEstado;
import org.openmrs.module.fua.api.FuaEstadoService;
import org.openmrs.web.WebConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping(value = "/module/fua/estado")
public class FuaEstadoController {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private FuaEstadoService fuaEstadoService;

    private final String FORM_VIEW = "/module/fua/pages/addFuaEstado";

    @RequestMapping(method = RequestMethod.GET)
    public String onGet(ModelMap model, @RequestParam(value = "estadoId", required = false) Integer estadoId) {
        FuaAccess.require(FuaConfig.READ_FUA_PRIVILEGE);
        FuaEstado estado = (estadoId != null) ? fuaEstadoService.getEstado(estadoId) : new FuaEstado();
        model.addAttribute("estado", estado);
        model.addAttribute("estados", fuaEstadoService.getAllEstados());
        return FORM_VIEW;
    }

    @RequestMapping(method = RequestMethod.POST)
    public String onPost(HttpSession httpSession, @ModelAttribute("estado") FuaEstado estado, BindingResult errors,
                         @RequestParam(required = false, value = "action") String action) {
        FuaAccess.require("purge".equals(action) ? FuaConfig.DELETE_FUA_PRIVILEGE : FuaConfig.MANAGE_FUA_PRIVILEGE);

        MessageSourceService mss = Context.getMessageSourceService();

        if (errors.hasErrors()) {
            return FORM_VIEW;
        }

        if (!Context.isAuthenticated()) {
            errors.reject("fua.auth.required");
        } else if ("purge".equals(action)) {
            try {
                fuaEstadoService.purgeEstado(estado);
                httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "fua.estado.delete.success");
            } catch (Exception ex) {
                httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "fua.estado.delete.failure");
                log.error("Error al eliminar FuaEstado", ex);
            }
        } else {
            fuaEstadoService.saveEstado(estado);
            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "fua.estado.saved");
        }
        return "redirect:/module/fua/estado";
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<FuaEstado> getAllEstados() {
        FuaAccess.require(FuaConfig.READ_FUA_PRIVILEGE);
        log.info("Llamada a /module/fua/estado/list");
        return fuaEstadoService.getAllEstados();
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> createEstado(@RequestBody FuaEstado nuevoEstado) {
        FuaAccess.require(FuaConfig.MANAGE_FUA_PRIVILEGE);
        if (!Context.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Debe autenticarse para crear el estado.");
		}
        log.info("Creando nuevo FuaEstado con nombre: " + nuevoEstado.getNombre());

        return ResponseEntity.ok(fuaEstadoService.saveEstado(nuevoEstado));
    }

}

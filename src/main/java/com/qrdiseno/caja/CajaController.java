
package com.qrdiseno.caja;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
public class CajaController {

    private final MovimientoRepository repository;

    public CajaController(MovimientoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String inicio(
            @RequestParam(required = false) String fecha,
            Model model) {

        LocalDate dia = LocalDate.now();

        if (fecha != null && !fecha.isBlank()) {
            try {
                dia = LocalDate.parse(fecha);
            } catch (DateTimeParseException e) {
                model.addAttribute("error", "La fecha ingresada no es válida.");
            }
        }

        model.addAttribute("movimientos",
                repository.findByFechaOrderByIdDesc(dia));
        model.addAttribute("fecha", dia);
        model.addAttribute("ingresos", calcularTotal("INGRESO", dia));
        model.addAttribute("egresos", calcularTotal("EGRESO", dia));

        return "index";
    }

    @PostMapping("/movimientos")
    public String guardar(
            @RequestParam String tipo,
            @RequestParam String concepto,
            @RequestParam BigDecimal importe,
            @RequestParam String medioPago,
            RedirectAttributes redirect) {

        if (!tipo.equals("INGRESO") && !tipo.equals("EGRESO")) {
            redirect.addFlashAttribute("error",
                    "Tipo de movimiento inválido.");
            return "redirect:/";
        }

        if (concepto.isBlank() || importe.compareTo(BigDecimal.ZERO) <= 0) {
            redirect.addFlashAttribute("error",
                    "Ingresá un concepto y un importe mayor que cero.");
            return "redirect:/";
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto.trim());
        movimiento.setImporte(importe);
        movimiento.setMedioPago(medioPago);
        movimiento.setFecha(LocalDate.now());

        repository.save(movimiento);

        return "redirect:/";
    }

    private BigDecimal calcularTotal(String tipo, LocalDate fecha) {
        return repository.findByFechaOrderByIdDesc(fecha).stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .map(Movimiento::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

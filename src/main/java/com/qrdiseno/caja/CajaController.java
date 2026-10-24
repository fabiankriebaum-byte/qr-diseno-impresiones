
package com.qrdiseno.caja;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

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

        if (!datosValidos(tipo, concepto, importe, medioPago)) {
            redirect.addFlashAttribute("error",
                    "Revisá los datos: concepto e importe son obligatorios, "
                    + "el importe debe ser mayor que cero.");
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

    @GetMapping("/movimientos/{id}/editar")
    public String mostrarEdicion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        Optional<Movimiento> resultado = repository.findById(id);

        if (resultado.isEmpty()) {
            redirect.addFlashAttribute("error",
                    "No se encontró ese movimiento.");
            return "redirect:/";
        }

        model.addAttribute("movimiento", resultado.get());
        return "editar-movimiento";
    }

    @PostMapping("/movimientos/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @RequestParam String tipo,
            @RequestParam String concepto,
            @RequestParam BigDecimal importe,
            @RequestParam String medioPago,
            RedirectAttributes redirect) {

        if (!datosValidos(tipo, concepto, importe, medioPago)) {
            redirect.addFlashAttribute("error",
                    "Revisá los datos ingresados.");
            return "redirect:/movimientos/" + id + "/editar";
        }

        Optional<Movimiento> resultado = repository.findById(id);

        if (resultado.isEmpty()) {
            redirect.addFlashAttribute("error",
                    "No se encontró ese movimiento.");
            return "redirect:/";
        }

        Movimiento movimiento = resultado.get();
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto.trim());
        movimiento.setImporte(importe);
        movimiento.setMedioPago(medioPago);

        repository.save(movimiento);

        return "redirect:/?fecha=" + movimiento.getFecha();
    }

    @PostMapping("/movimientos/{id}/eliminar")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        Optional<Movimiento> resultado = repository.findById(id);

        if (resultado.isEmpty()) {
            redirect.addFlashAttribute("error",
                    "No se encontró ese movimiento.");
            return "redirect:/";
        }

        LocalDate fecha = resultado.get().getFecha();
        repository.delete(resultado.get());

        redirect.addFlashAttribute("mensaje",
                "Movimiento eliminado correctamente.");

        return "redirect:/?fecha=" + fecha;
    }

    private boolean datosValidos(
            String tipo, String concepto,
            BigDecimal importe, String medioPago) {

        return tipo != null
                && (tipo.equals("INGRESO") || tipo.equals("EGRESO"))
                && concepto != null
                && !concepto.isBlank()
                && importe != null
                && importe.compareTo(BigDecimal.ZERO) > 0
                && medioPago != null
                && (medioPago.equals("EFECTIVO")
                    || medioPago.equals("TRANSFERENCIA")
                    || medioPago.equals("OTRO"));
    }

    private BigDecimal calcularTotal(String tipo, LocalDate fecha) {
        return repository.findByFechaOrderByIdDesc(fecha).stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .map(Movimiento::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

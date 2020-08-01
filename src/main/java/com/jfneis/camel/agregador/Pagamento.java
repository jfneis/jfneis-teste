package com.jfneis.camel.agregador;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Pagamento
{
	private List<Registro> registros = new ArrayList<>();
	
	public double getValorTotal()
	{
		return registros.stream().mapToDouble(r -> r.getValor()).sum();
	}
}

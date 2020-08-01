package com.jfneis.camel.agregador;

import lombok.Builder;
import lombok.Data;

@Builder
@Data 
public class Registro
{
	private String referencia;
	private String tipo;
	private Integer numeroParcela;
	private Double valor;
}
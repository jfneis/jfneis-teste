package com.jfneis.camel.agregador;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.builder.PredicateBuilder.not;

public class AgregadorRoute extends RouteBuilder
{
	@Override
	public void configure() throws Exception
	{
		//// @formatter:off
		from("direct:agregadores")
			.routeId("agregadores")
			.process(this::carregarPagamentos)
	        .split(body())
	        .log("entrando no aggregate")
	        .aggregate(simple("${body.referencia}"), this::agregadorMesmaReferencia).completionTimeout(2000).forceCompletionOnStop()
		        .log("saindo do aggregate")
			    .choice()
			    	.when(not(header("PREPAG").isEqualTo(true)))
			    		.to("direct:pagamentos")
			    	.otherwise()
			    		.to("direct:pre-pagamentos")
			    .end();
		    		
		from("direct:pagamentos")
			.routeId("pagamentos")
			.split(body())
			.aggregate(simple("${body.referencia}-${body.numeroParcela}"), this::agregadorPagamentos).completionTimeout(2000).forceCompletionOnStop()
			.log(LoggingLevel.WARN, "${body}");
		
		from("direct:pre-pagamentos")
			.routeId("pre-pagamentos")
			.split(body())
			.aggregate(simple("${body.referencia}"), this::agregadorPagamentos).completionTimeout(2000).forceCompletionOnStop()
			.log(LoggingLevel.WARN, "${body}");
		// @formatter:on
	}
	
	public Exchange agregadorMesmaReferencia(Exchange oldExchange, Exchange newExchange)
	{
		List<Registro> registros = oldExchange != null ? (List<Registro>)oldExchange.getIn().getBody() : new ArrayList<>();
		Exchange exchange = oldExchange == null ? newExchange : oldExchange;
		
		Registro registro = newExchange.getIn().getBody(Registro.class);
		registros.add(registro);
		exchange.getIn().setBody(registros);

		// seta um header pra depois agregar diferente só os não PREPAGs
		if (registro.getTipo().equals("PREPAG"))
			exchange.getIn().setHeader("PREPAG", true);

		return exchange;
	}
	
	public Exchange agregadorPagamentos(Exchange oldExchange, Exchange newExchange)
	{
		// aqui vai a lógica atual de juntar principal e juros, etc.
		
		Pagamento pgto = oldExchange == null ? new Pagamento() : oldExchange.getIn().getBody(Pagamento.class);
		
		pgto.getRegistros().add(newExchange.getIn().getBody(Registro.class));
		
		newExchange.getIn().setBody(pgto);
		
		return newExchange;
	}

	private void carregarPagamentos(Exchange e)
	{
		List<Registro> registros = new ArrayList<>();
		
		// pagamento normal (só 1 parc)
		registros.add(Registro.builder().referencia("a").tipo("PRINCIPAL").numeroParcela(1).valor(100d).build());
		registros.add(Registro.builder().referencia("a").tipo("JUROS").numeroParcela(1).valor(100d).build());

		// pagamento adiantado
		registros.add(Registro.builder().referencia("b").tipo("PRINCIPAL").numeroParcela(1).valor(100d).build());
		registros.add(Registro.builder().referencia("b").tipo("JUROS").numeroParcela(1).valor(100d).build());
		registros.add(Registro.builder().referencia("b").tipo("PREPAG").numeroParcela(1).valor(0d).build());
		registros.add(Registro.builder().referencia("b").tipo("PREPAG").numeroParcela(2).valor(0d).build());
		registros.add(Registro.builder().referencia("b").tipo("PREPAG").numeroParcela(3).valor(0d).build());
		
		// pagamento de 2 parcelas do mesmo contrato (sem adiantar)
		registros.add(Registro.builder().referencia("c").tipo("PRINCIPAL").numeroParcela(1).valor(100d).build());
		registros.add(Registro.builder().referencia("c").tipo("JUROS").numeroParcela(1).valor(100d).build());
		registros.add(Registro.builder().referencia("c").tipo("PRINCIPAL").numeroParcela(2).valor(100d).build());
		registros.add(Registro.builder().referencia("c").tipo("JUROS").numeroParcela(2).valor(100d).build());
		
		e.getIn().setBody(registros);
	}

}
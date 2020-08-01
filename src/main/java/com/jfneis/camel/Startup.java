package com.jfneis.camel;

import java.util.HashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jfneis.camel.agregador.AgregadorRoute;

public class Startup
{
	private final ProducerTemplate template;
	private final CamelContext context;
	
	public Startup() throws Exception
	{
		JndiContext jndiContext = new JndiContext();
		jndiContext.bind("json-jackson", jacksonDataFormat());
		context = new DefaultCamelContext(jndiContext);
		context.addRoutes(new AgregadorRoute());
		template = context.createProducerTemplate();
		context.start();		
	}
	
	private JacksonDataFormat jacksonDataFormat()
	{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return new JacksonDataFormat(objectMapper, HashMap.class);
	}
	
	public void testeAgregadores() throws Exception
	{
		template.requestBody("direct:agregadores", "abc");
		
		// sem parar a rota os agregadores não liberam os exchanges pro próximo passo
		context.stop();
	}
	
	public static void main(String... args) throws Exception
	{
		Startup startup = new Startup();
		startup.testeAgregadores();
	}	
}
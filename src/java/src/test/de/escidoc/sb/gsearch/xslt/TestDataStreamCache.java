package src.test.de.escidoc.sb.gsearch.xslt;

import static org.junit.Assert.*;

import org.junit.Test;

import dk.defxws.fedoragsearch.server.DataStreamCache;

public class TestDataStreamCache {
	
	DataStreamCache cache = DataStreamCache.getInstance();

	@Test
	public void test() {
		cache.put("escidoc:1111", 
				"/ir/item/escidoc:1111:4/components/component/escidoc:2060737/content", 
				"thread-1", "Da steht der Volltext");
		
		cache.put("escidoc:1111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-2", "Da steht ein anderer der Volltext");
		
		cache.put("escidoc:2111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-1", "Volltext von escidoc:2111");
		
		cache.put("escidoc:2111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-12", "Volltext von escidoc:2111 thread-12");
		
		assertTrue(cache.size() == 4);
		
		String s = cache.get("escidoc:1111", 
				"/ir/item/escidoc:1111:4/components/component/escidoc:2060737/content", 
				"thread-1");

		assertTrue(s.equals("Da steht der Volltext"));
		
		s = cache.get("escidoc:1111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-2");
		assertTrue(s.equals("Da steht ein anderer der Volltext"));
		
		s = cache.get("escidoc:2111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-1");
		assertTrue(s.equals("Volltext von escidoc:2111"));
		
		s = cache.get("escidoc:2111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-12");
		assertTrue(s.equals("Volltext von escidoc:2111 thread-12"));
		
		// not found
		assertFalse(cache.containsKey("xxx", "xxxxx", "yyyyyy"));
		
		s = cache.get("escidoc:2XXX", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-12");
		assertTrue(s.equals(""));
		
		// UTF-8
		cache.put("escidoc:3111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-12", "Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ");
		
		s = cache.get("escidoc:3111", 
				"/ir/item/escidoc:escidoc:1111/components/component/escidoc:2060737/content", 
				"thread-12");
		assertTrue(s.equals("Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ"));
	}

}

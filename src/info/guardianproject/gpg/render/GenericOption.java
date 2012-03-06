package info.guardianproject.gpg.render;

import java.util.HashMap;

public class GenericOption {
	String label;
	Object primaryValue;
	HashMap<String, Object> extras;
	
	public GenericOption(String label, Object primaryValue) {
		this.label = label;
		this.primaryValue = primaryValue;
		this.extras = null;
	}
	
	public void addExtra(String key, Object value) {
		if(extras == null)
			extras = new HashMap<String, Object>();
		
		extras.put(key, value);
	}
	
	public HashMap<String, Object> getExtras() {
		return extras;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Object getPrimaryValue() {
		return primaryValue;
	}
}

package org.openhds.test.model;

public class PlainElement extends AbstractElement {

	private String value;
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public PlainElement(ElementType type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

}

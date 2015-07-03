package org.openhds.test.model;

import java.util.ArrayList;
import java.util.List;

public class StructuredElement extends AbstractElement {

	private List<ElementInterface> childElements = new ArrayList<ElementInterface>();
	
	public List<ElementInterface> getChildElements() {
		return this.childElements;
	}

	public void setChildElements(List<ElementInterface> childElements) {
		if(this.getElementType() == ElementInterface.ElementType.GROUP || 
				this.getElementType() == ElementInterface.ElementType.GEOPOINT){
			this.childElements = childElements;
		}
		else{
			System.err.println("Can only add child elements for type GROUP or GEOPOINT");
			System.exit(0);
		}
	}

	public StructuredElement(ElementType type) {
		super(type);
	}
	
	

}

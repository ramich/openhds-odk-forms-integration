package org.openhds.test.model;


public class AbstractElement implements ElementInterface {

	private ElementType type;
    private FormDataModel formDataModel;
	
	public AbstractElement(ElementType type){
		setElementType(type);
	}
	
	@Override
	public ElementType getElementType() {
		return this.type;
	}

	@Override
	public void setElementType(ElementType type) {
		this.type = type;
	}

	public FormDataModel getFormDataModel() {
		return formDataModel;
	}

	public void setFormDataModel(FormDataModel formDataModel) {
		this.formDataModel = formDataModel;
	}

}

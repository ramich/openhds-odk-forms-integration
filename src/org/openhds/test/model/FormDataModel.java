package org.openhds.test.model;

import java.util.ArrayList;
import java.util.List;

public class FormDataModel {
	
	private String _URI, _CREATOR_URI_USER, _CREATION_DATE, _LAST_UPDATE_URI_USER, _LAST_UPDATE_DATE, URI_SUBMISSION_DATA_MODEL, PARENT_URI_FORM_DATA_MODEL, ORDINAL_NUMBER, ELEMENT_TYPE, ELEMENT_NAME, PERSIST_AS_COLUMN_NAME, PERSIST_AS_TABLE_NAME, PERSIST_AS_SCHEMA_NAME;
	private List<FormDataModel> children = new ArrayList<FormDataModel>();

	public String get_URI() {
		return _URI;
	}

	public void set_URI(String _URI) {
		this._URI = _URI;
	}

	public String get_CREATOR_URI_USER() {
		return _CREATOR_URI_USER;
	}

	public void set_CREATOR_URI_USER(String _CREATOR_URI_USER) {
		this._CREATOR_URI_USER = _CREATOR_URI_USER;
	}

	public String get_CREATION_DATE() {
		return _CREATION_DATE;
	}

	public void set_CREATION_DATE(String _CREATION_DATE) {
		this._CREATION_DATE = _CREATION_DATE;
	}

	public String get_LAST_UPDATE_URI_USER() {
		return _LAST_UPDATE_URI_USER;
	}

	public void set_LAST_UPDATE_URI_USER(String _LAST_UPDATE_URI_USER) {
		this._LAST_UPDATE_URI_USER = _LAST_UPDATE_URI_USER;
	}

	public String get_LAST_UPDATE_DATE() {
		return _LAST_UPDATE_DATE;
	}

	public void set_LAST_UPDATE_DATE(String _LAST_UPDATE_DATE) {
		this._LAST_UPDATE_DATE = _LAST_UPDATE_DATE;
	}

	public String getURI_SUBMISSION_DATA_MODEL() {
		return URI_SUBMISSION_DATA_MODEL;
	}

	public void setURI_SUBMISSION_DATA_MODEL(String uRI_SUBMISSION_DATA_MODEL) {
		URI_SUBMISSION_DATA_MODEL = uRI_SUBMISSION_DATA_MODEL;
	}

	public String getPARENT_URI_FORM_DATA_MODEL() {
		return PARENT_URI_FORM_DATA_MODEL;
	}

	public void setPARENT_URI_FORM_DATA_MODEL(String pARENT_URI_FORM_DATA_MODEL) {
		PARENT_URI_FORM_DATA_MODEL = pARENT_URI_FORM_DATA_MODEL;
	}

	public String getORDINAL_NUMBER() {
		return ORDINAL_NUMBER;
	}

	public void setORDINAL_NUMBER(String oRDINAL_NUMBER) {
		ORDINAL_NUMBER = oRDINAL_NUMBER;
	}

	public String getELEMENT_TYPE() {
		return ELEMENT_TYPE;
	}

	public void setELEMENT_TYPE(String eLEMENT_TYPE) {
		ELEMENT_TYPE = eLEMENT_TYPE;
	}

	public String getELEMENT_NAME() {
		return ELEMENT_NAME;
	}

	public void setELEMENT_NAME(String eLEMENT_NAME) {
		ELEMENT_NAME = eLEMENT_NAME;
	}

	public String getPERSIST_AS_COLUMN_NAME() {
		return PERSIST_AS_COLUMN_NAME;
	}

	public void setPERSIST_AS_COLUMN_NAME(String pERSIST_AS_COLUMN_NAME) {
		PERSIST_AS_COLUMN_NAME = pERSIST_AS_COLUMN_NAME;
	}

	public String getPERSIST_AS_TABLE_NAME() {
		return PERSIST_AS_TABLE_NAME;
	}

	public void setPERSIST_AS_TABLE_NAME(String pERSIST_AS_TABLE_NAME) {
		PERSIST_AS_TABLE_NAME = pERSIST_AS_TABLE_NAME;
	}

	public String getPERSIST_AS_SCHEMA_NAME() {
		return PERSIST_AS_SCHEMA_NAME;
	}

	public void setPERSIST_AS_SCHEMA_NAME(String pERSIST_AS_SCHEMA_NAME) {
		PERSIST_AS_SCHEMA_NAME = pERSIST_AS_SCHEMA_NAME;
	}
	
	public List<FormDataModel> getChildren(){
		return this.children;
	}
	
	public void addChild(FormDataModel child){
		children.add(child);
	}
	
	public void setChildren(List<FormDataModel> children){
		this.children = children;
	}

}

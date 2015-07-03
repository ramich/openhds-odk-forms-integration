package org.openhds.test.model.mirth;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="table")
public class TableDummy {

	private String name;
	private String key;
	private String primaryKey;
	private List<ColumnDummy> columns;
	
	public TableDummy(){
		columns = new ArrayList<ColumnDummy>();
	}

	public String getName() {
		return name;
	}

	@XmlAttribute
	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	@XmlAttribute
	public void setKey(String key) {
		this.key = key;
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	@XmlAttribute
	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	public List<ColumnDummy> getColumns() {
		return columns;
	}

	@XmlElement
	public void setColumns(List<ColumnDummy> columns) {
		this.columns = columns;
	}
	
	public void addColumn(ColumnDummy column){
		this.columns.add(column);
	}
}

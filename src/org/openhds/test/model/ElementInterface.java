package org.openhds.test.model;

public interface ElementInterface {

    /* xform element types */
    public static enum ElementType {
            // xform tag types
            STRING,
            JRDATETIME,
            JRDATE,
            JRTIME,
            INTEGER,
            DECIMAL,
            GEOPOINT,
            BINARY,  // identifies BinaryContent table
            BOOLEAN,
            SELECT1, // identifies SelectChoice table
            SELECTN, // identifies SelectChoice table
            REPEAT,
            GROUP,
            // additional supporting tables
            PHANTOM, // if a relation needs to be divided in order to fit
            BINARY_CONTENT_REF_BLOB, // association between BINARY and REF_BLOB
            REF_BLOB, // the table of the actual byte[] data (xxxBLOB)
            LONG_STRING_REF_TEXT, // association between any field and REF_TEXT
            REF_TEXT, // the table of extended string values (xxxTEXT)
    };
        
    ElementType getElementType();
    void setElementType(ElementType type);
    
}

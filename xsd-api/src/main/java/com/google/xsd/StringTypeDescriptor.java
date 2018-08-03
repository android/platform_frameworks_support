package com.google.xsd;

class StringTypeDescriptor extends SimpleTypeDescriptor {
    StringTypeDescriptor(boolean isList) {
        super("java.lang.String", "java.lang.String", isList);
    }

    protected String getRawParsingExpression(String varName) {
        return varName;
    }
}

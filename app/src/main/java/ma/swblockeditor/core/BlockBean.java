package ma.swblockeditor.core;

import java.util.ArrayList;

public class BlockBean {
    private String opCode = "";
    private String headerText = "";
    private int color = 0;
    private String type = "";
    private String code = "";
    private String spec = "";
    private String spec2 = "";
    private ArrayList<String> tokenizedSpec = new ArrayList<>();
    private ArrayList<String> parameters = new ArrayList<>();

    public String getOpCode() {
        return opCode;
    }

    public BlockBean setOpCode(String opCode) {
        this.opCode = opCode != null ? opCode : "";
        return this;
    }

    public String getHeaderText() {
        return headerText;
    }

    public BlockBean setHeaderText(String headerText) {
        this.headerText = headerText != null ? headerText : "";
        return this;
    }

    public int getColor() {
        return color;
    }

    public BlockBean setColor(int color) {
        this.color = color;
        return this;
    }

    public String getType() {
        return type;
    }

    public BlockBean setType(String type) {
        this.type = type != null ? type : "";
        return this;
    }

    public String getCode() {
        return code;
    }

    public BlockBean setCode(String code) {
        this.code = code != null ? code : "";
        return this;
    }

    public String getSpec() {
        return spec;
    }

    public BlockBean setSpec(String spec) {
        this.spec = spec != null ? spec : "";
        return this;
    }

    public String getSpec2() {
        return spec2;
    }

    public BlockBean setSpec2(String spec2) {
        this.spec2 = spec2 != null ? spec2 : "";
        return this;
    }

    public ArrayList<String> getTokenizedSpec() {
        return tokenizedSpec;
    }

    public BlockBean setTokenizedSpec(ArrayList<String> tokenizedSpec) {
        this.tokenizedSpec = tokenizedSpec != null ? tokenizedSpec : new ArrayList<>();
        return this;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public BlockBean setParameters(ArrayList<String> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        return this;
    }
}

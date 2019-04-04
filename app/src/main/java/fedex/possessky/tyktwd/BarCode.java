package fedex.possessky.tyktwd;

public class BarCode {
    private String publicID;
    private String cityID;
    private int intCode;
    BarCode(){
    }
    public void setCode(int code){
        publicID = Integer.toString(code);
        intCode = code;
    }

    public void setCity(String city){
        cityID = city;
    }

    public String getCity(){
        return cityID;
    }

    public int getIntCode(){
        return intCode;
    }

    public String getCode(){
        return publicID;
    }



}

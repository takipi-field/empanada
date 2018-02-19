

public class SayHello {

    public static void main(String[] args) {
        System.out.println("Hello, World"+System.getenv("LAMBDA_RUNTIME_DIR"));
        try{
          Thread.sleep(1000);
          System.out.println("lalalala");
          try{
            throw new Exception("Hello Exception");
          }catch(Exception e){
            e.printStackTrace();
          }
          Thread.sleep(1000); 
        }catch(Exception e){
          e.printStackTrace();
        }
    }

}

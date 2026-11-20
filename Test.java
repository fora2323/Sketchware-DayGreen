import org.sketchware.daygreen.ai.AiProviders;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Test {
    public static void main(String[] args) {
        for (Field f : AiProviders.Provider.class.getDeclaredFields()) {
            System.out.println(f.getName() + \" \" + Modifier.toString(f.getModifiers()));
        }
    }
}

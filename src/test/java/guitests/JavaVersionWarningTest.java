package guitests;

import javafx.application.Platform;
import org.junit.Test;
import ui.TestController;
import ui.UI;
import util.JavaVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.fail;
import static org.loadui.testfx.controls.Commons.hasText;

public class JavaVersionWarningTest extends UITest {

    @Test
    public void javaVersionWarning_OutdateJavaRuntime_WarningDialogAppears() {
        UI ui = TestController.getUI();

        JavaVersion runtime = new JavaVersion(0, 0, 0, 0, 0);
        JavaVersion required = new JavaVersion(1, 0, 0, 0, 0);

        Method methodCheckJavaDependency;

        try {
            methodCheckJavaDependency = UI.class.getDeclaredMethod("checkJavaDependency",
                    JavaVersion.class, JavaVersion.class);
            methodCheckJavaDependency.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            fail("Can't reflect method");
            return;
        }

        Platform.runLater(() -> {
            try {
                methodCheckJavaDependency.invoke(ui, runtime, required);
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to call method");
            }
        });

        String message = "Your Java version is older than HubTurbo's requirement. " +
                "Use it at your own risk.\n\n" +
                "Required version\t: " + required.toString() + "\n" +
                "Installed version\t: " + runtime.toString();

        waitUntilNodeAppears(hasText(message));
        click("OK");
    }
}

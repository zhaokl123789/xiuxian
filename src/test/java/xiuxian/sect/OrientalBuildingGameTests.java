package xiuxian.sect;

import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_geometry")
@PrefixGameTestTemplate(false)
public final class OrientalBuildingGameTests {
    @GameTest(templateNamespace = "xiuxian_geometry", template = "empty", timeoutTicks = 400)
    public static void bothBuildingsPreserveDecorationsAndRoutes(GameTestHelper helper) {
        try {
            LuoxiaGeometryVerification.verifyRegistered(Path.of(System.getProperty("xiuxian.verificationRoot", "../..")));
            helper.succeed();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}

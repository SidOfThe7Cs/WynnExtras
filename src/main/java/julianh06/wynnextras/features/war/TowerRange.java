package julianh06.wynnextras.features.war;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.render.CircleRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;

@WEModule
public class TowerRange {
    private static final CircleRenderer circleRenderer = new CircleRenderer();
    private static final Color CIRCLE_COLOR = new Color(255, 0, 0, 200);
    private static final Color NEXT_CIRCLE_COLOR = new Color(255, 0, 0, 100);
    private Vec3d potentialTowerLocation = null;

    private Vec3d lastWorldPos = null;
    private boolean waitingForWorldLoad = false;

    @SubscribeEvent
    public void onWorldSwitch(WorldChangeEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        lastWorldPos = player.getEntityPos();
        waitingForWorldLoad = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (waitingForWorldLoad) {
            Vec3d playerPos = MinecraftUtils.localPlayer().getEntityPos();
            if (playerPos == lastWorldPos) return;
            potentialTowerLocation = playerPos.add(0, 0, 30);
            waitingForWorldLoad = false;
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldEvent event) {
        if (WarDPS.isInWar()) {
            Vec3d playerPos = MinecraftUtils.localPlayer().getEntityPos();
            long timeInWar = WarDPS.getTimeSinceWarStart() / 1000;
            int increases = (int) timeInWar / 120;
            int nextIncreases = (int) (timeInWar + 10) / 120;

            Vec3d actuallyTowerPos = new Vec3d(potentialTowerLocation.x, getGroundPosition(playerPos).y, potentialTowerLocation.z);

            circleRenderer.queueCircle(actuallyTowerPos, 31 + increases, 1, CIRCLE_COLOR);
            if (nextIncreases > increases) circleRenderer.queueCircle(actuallyTowerPos, 31 + nextIncreases, 1, NEXT_CIRCLE_COLOR);
        }

        circleRenderer.renderQueuedCircles(event);
    }

    public static Vec3d getGroundPosition(Vec3d pos) {
        return getGroundPosition(pos, -64); // Default to world bottom limit
    }

    public static Vec3d getGroundPosition(Vec3d pos, int minY) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return null;

        // Start from the given Y coordinate and go down
        int startY = (int) Math.floor(pos.y);

        for (int y = startY; y >= minY; y--) {
            BlockPos checkPos = new BlockPos((int) Math.floor(pos.x), y, (int) Math.floor(pos.z));
            BlockState state = world.getBlockState(checkPos);

            if (isSolidBlock(state)) {
                // Return position at top of this solid block
                return new Vec3d(pos.x, y + 1, pos.z);
            }
        }

        // No solid block found, return original position or world spawn
        return null;
    }

    private static boolean isSolidBlock(BlockState state) {
        return state.isSolid() && !state.isAir();
    }
}

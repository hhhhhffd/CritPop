import com.critpop.CritTracker;
import com.critpop.PopPlacement;

public class CritTrackerTest {
    private static int passed;
    private static void check(boolean condition, String test) {
        if (!condition) throw new AssertionError(test);
        passed++;
    }

    private static void sound(CritTracker tracker, boolean critical, long ms, int x) {
        tracker.sound(critical, ms, x, 64, 0, 0, 64, 0);
    }

    public static void main(String[] args) {
        CritTracker tracker = new CritTracker();

        tracker.attack(8, 0, 0, 64, 0);
        check(!tracker.critAnimation(8, 10), "client intention alone is not enough");
        tracker.damage(8, true, 30);
        check(!tracker.critAnimation(8, 31), "must see critical sound as well");
        sound(tracker, true, 32, 0);
        check(!tracker.critAnimation(9, 33), "animation must match the attack target");
        check(tracker.critAnimation(8, 34), "proper three-packet critical chain");
        check(!tracker.critAnimation(8, 35), "duplicate animation ignored");

        tracker.attack(8, 100, 0, 64, 0);
        tracker.damage(8, true, 150);
        sound(tracker, false, 151, 0);
        sound(tracker, true, 152, 0);
        check(!tracker.critAnimation(8, 153), "ground hit followed by another nearby critical");
        tracker.attack(8, 200, 0, 64, 0);
        tracker.damage(8, false, 240);
        sound(tracker, true, 241, 0);
        check(!tracker.critAnimation(8, 242), "not own direct melee damage");

        tracker.attack(8, 300, 0, 64, 0);
        tracker.damage(8, true, 340);
        sound(tracker, true, 341, 5);
        check(!tracker.critAnimation(8, 342), "distant player's sound doesn't confirm");
        sound(tracker, true, 343, 0);
        tracker.damage(8, false, 344);
        check(!tracker.critAnimation(8, 345), "other player's damage cancels the candidate");

        tracker.attack(8, 400, 0, 64, 0);
        tracker.damage(8, true, 2500);
        sound(tracker, true, 2501, 0);
        check(!tracker.critAnimation(8, 2502), "old attack can't confirm");
        tracker.attack(8, 2600, 0, 64, 0);
        tracker.damage(8, true, 2700);
        sound(tracker, true, 2701, 0);
        check(!tracker.critAnimation(8, 2950), "old critical packets can't confirm");
        tracker.attack(8, 3000, 0, 64, 0);
        tracker.damage(8, true, 3030);
        sound(tracker, true, 3031, 0);
        tracker.clear();
        check(!tracker.critAnimation(8, 3032), "respawn or server changes clear attribution");

        for (int width : new int[] {160, 320, 640, 1280}) {
            for (int height : new int[] {160, 240, 360, 720}) {
                for (int zone = 0; zone < PopPlacement.ZONES.length; zone++) {
                    float scale = PopPlacement.scale(width, height, 168);
                    float radiusX = (84 + 8 + 13) * scale + 5;
                    float radiusY = 19 * scale;
                    float x = PopPlacement.x(width, zone, radiusX);
                    float y = PopPlacement.y(height, zone, 1, radiusY);
                    check(x >= radiusX && x <= width - radiusX, "horizontal clamp");
                    check(y >= radiusY && y + radiusY <= PopPlacement.ceiling(height) + 0.001,
                            "pop stays entirely above center");
                }
            }
        }
        System.out.println("Passed " + passed + " attribution and placement checks.");
    }
}

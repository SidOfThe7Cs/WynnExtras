package julianh06.wynnextras.event;

import com.wynntils.core.components.Models;
import julianh06.wynnextras.event.api.WEEvent;

import java.util.Objects;

public class CharacterIdChangeEvent extends WEEvent {
    public final String newId;

    public CharacterIdChangeEvent(String newId) {
        this.newId = newId;
    }

    private static String lastId = "-";
    public static void onClientTick() {
        try {
            String id = Models.Character.getId();
            if (!Objects.equals(lastId, id)) {
                new CharacterIdChangeEvent(id).post();
            }
            lastId = id;
        } catch (Exception ignored) { }
    }
}

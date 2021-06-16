package timetracker.updates;

/**
 * Interface f�r alle Updates
 */
public interface IUpdateMethod
{
    /**
     * Liefert eine eindeutige UpdateId
     * @return UpdateId
     */
    int getUpdateId();

    /**
     * Gibt an, ob das Update vor dem Initialisieren des Backends ausgef�hrt werden soll
     * @return {@code true}, wenn das Update vor dem Initialisieren des Backends ausgef�hrt werden soll, sonst {@code false}
     */
    boolean isPreBackendUpdate();

    /**
     * F�hrt das eigentliche Update aus
     * @return {@code true}, wenn das Update erfolgreich war, sonst {@code false}
     */
    boolean doUpdate();
}
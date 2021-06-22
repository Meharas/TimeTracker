package timetracker.updates;

import timetracker.db.Backend;
import timetracker.log.Log;
import timetracker.utils.Util;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/**
 * Sammelt alle Updates
 */
public final class Updates
{
    private static final Collection<Class<? extends IUpdateMethod>> UPDATE_METHODS = new LinkedList<>();
    static
    {
        UPDATE_METHODS.add(CopyUserFiles.class);
        UPDATE_METHODS.add(RenameColumnDeletable.class);
        UPDATE_METHODS.add(UpdateOrders.class);
    }

    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        static final Updates instance = new Updates();
    }

    public static Updates getInstance()
    {
        return Updates.InstanceHolder.instance;
    }

    /**
     * Führt alle Updates aus, welche vor der Initiolisierung des Backends ausgeführt werden sollen.
     * D.h. aber auch, dass diese Updates nicht in der Update-Tabelle vermerkt werden und das die Updates selbst sicher stellen müssen,
     * dass sie nicht bei jedem Start das gleiche ausführen.
     */
    public void executePreBackendUpdates()
    {
        Log.info("Executing pre backend updates");
        for (final Class<? extends IUpdateMethod> updateMethod : UPDATE_METHODS)
        {
            try
            {
                final Constructor<? extends IUpdateMethod> constructor = updateMethod.getConstructor();
                final IUpdateMethod update = constructor.newInstance();
                if(!update.isPreBackendUpdate())
                {
                    continue;
                }
                final int updateId = update.getUpdateId();

                Log.info("Executing update " + update.getClass().getSimpleName());
                final boolean successful = update.doUpdate();
                if (successful)
                {
                    Log.info(String.format("Update #%d successful.", updateId));
                    continue;
                }
                Log.severe(String.format("Update #%d failed.", updateId));
            }
            catch (final Exception e)
            {
                Util.handleException(e);

            }
        }
    }

    /**
     * Führt alle registrierten Updates aus, insofern diese nicht schon ausgeführt wurden.
     */
    public void executeUpdates()
    {
        final Backend backend = Backend.getInstance();
        final Set<Integer> updateIds = backend.getUpdateIds();
        for(final Class<? extends IUpdateMethod> updateMethod : UPDATE_METHODS)
        {
            try
            {
                final Constructor<? extends IUpdateMethod> constructor = updateMethod.getConstructor();
                final IUpdateMethod update = constructor.newInstance();
                if(update.isPreBackendUpdate())
                {
                    continue;
                }
                final int updateId = update.getUpdateId();
                if(updateIds.add(updateId))
                {
                    backend.insertUpdate(update);
                    final boolean successful = update.doUpdate();
                    if(successful)
                    {
                        Log.info(String.format("Update #%d successful.", updateId));
                        backend.commit();
                        continue;
                    }
                    Log.severe(String.format("Update #%d failed.", updateId));
                    backend.rollback();
                }
            }
            catch (final Exception e)
            {
                Util.handleException(e);
            }
        }
    }
}
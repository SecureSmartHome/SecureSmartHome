package de.unipassau.isl.evs.ssh.master.database;

import android.content.Context;
import android.test.InstrumentationTestCase;

import junit.framework.Assert;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This test injects HolidayActions into the database for testing the holiday mode.
 * For this test to work, you need to have a slave with a light module installed at the master.
 */
public class HolidayModeTest extends InstrumentationTestCase {
    public void testHolidayMode() throws UnknownReferenceException {
        Context context = getInstrumentation().getTargetContext();
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(context));
        container.register(DatabaseConnector.KEY, new DatabaseConnector());
        container.register(HolidayController.KEY, new HolidayController());
        container.register(SlaveController.KEY, new SlaveController());

        final SlaveController slaveController = container.require(SlaveController.KEY);
        final HolidayController holidayController = container.require(HolidayController.KEY);

        List<Module> moduleList = slaveController.getModules();
        Module lightModule = null;

        for (Module module : moduleList) {
            if (module.getModuleType().equals(CoreConstants.ModuleType.Light)) {
                lightModule = module;
            }
        }
        if (lightModule == null) {
            Assert.fail("No light module!");
        }
        final long sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        holidayController.addHolidayLogEntry(CoreConstants.LogActions.LIGHT_ON_ACTION, lightModule.getName(), sevenDaysAgo + TimeUnit.SECONDS.toMillis(20));
        holidayController.addHolidayLogEntry(CoreConstants.LogActions.LIGHT_OFF_ACTION, lightModule.getName(), sevenDaysAgo + TimeUnit.SECONDS.toMillis(25));
        holidayController.addHolidayLogEntry(CoreConstants.LogActions.LIGHT_ON_ACTION, lightModule.getName(), sevenDaysAgo + TimeUnit.SECONDS.toMillis(30));
        holidayController.addHolidayLogEntry(CoreConstants.LogActions.LIGHT_OFF_ACTION, lightModule.getName(), sevenDaysAgo + TimeUnit.SECONDS.toMillis(35));
    }
}

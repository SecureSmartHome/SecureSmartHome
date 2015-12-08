package de.unipassau.isl.evs.ssh.master.task;

/**
 * Tasks are operations that are executed periodically by a scheduler.
 */
public interface Task extends Runnable {

    /**
     * Function wish will be run if task is due.
     */
    void run();

}
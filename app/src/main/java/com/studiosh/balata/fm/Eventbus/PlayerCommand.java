package com.studiosh.balata.fm.Eventbus;

public class PlayerCommand {
    public enum CommandEnum {
        PLAY, STOP, PAUSE
    }

    public CommandEnum mCommand;

    public PlayerCommand(CommandEnum command) {
        this.mCommand = command;
    }
}

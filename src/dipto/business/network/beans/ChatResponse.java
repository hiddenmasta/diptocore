package dipto.business.network.beans;

import java.io.Serializable;

public class ChatResponse implements Serializable
{
    private final boolean response;

    public ChatResponse(boolean response)
    {
        this.response = response;
    }

    public boolean getResponse()
    {
        return this.response;
    }
}
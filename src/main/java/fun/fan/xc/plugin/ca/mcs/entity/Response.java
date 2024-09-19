package fun.fan.xc.plugin.ca.mcs.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Response<T> {
    private ResHead resHead;
    private T resBody;
}

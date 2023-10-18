package dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccessGrantDTO implements Serializable {
    public String id;
    public byte[] msg;
    public byte[] digitalSignature;
    public ResourceEnum resourceSelected;
}

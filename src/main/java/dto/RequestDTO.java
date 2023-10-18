package dto;

import lombok.*;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class RequestDTO implements Comparable<RequestDTO>, Serializable {
    String id;
    long timestamp;
    byte[] msg;
    byte[] digitalSignature;
    ResourceEnum resourceSelected;

    @Override
    public int compareTo(RequestDTO o) {
        return (int) (this.timestamp - o.getTimestamp());
    }
}

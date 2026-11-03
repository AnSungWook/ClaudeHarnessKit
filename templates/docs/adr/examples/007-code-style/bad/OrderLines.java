// ADR-007 위반: 와일드카드 import - 무엇을 쓰는지 안 보이고, 같은 이름의 타입이 생기면 충돌한다
import java.util.*;
import java.util.stream.*;

public record OrderLines(List<OrderLine> lines) {

    // ...

    public Map<String, Integer> quantityByProductCode() {
        return lines.stream()
            .collect(Collectors.toUnmodifiableMap(OrderLine::productCode, OrderLine::quantity, Integer::sum));
    }
}

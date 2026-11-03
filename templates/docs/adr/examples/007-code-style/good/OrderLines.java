import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OrderLines(List<OrderLine> lines) {

    // ...

    public Map<String, Integer> quantityByProductCode() {
        return lines.stream()
            .collect(Collectors.toUnmodifiableMap(OrderLine::productCode, OrderLine::quantity, Integer::sum));
    }
}

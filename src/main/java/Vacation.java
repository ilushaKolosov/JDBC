import lombok.Data;

import java.util.Date;

@Data
class Vacation {
    private int vacationId;
    private String employeeName;
    private Date startDate;
    private Date endDate;
    private String vacationType;
    private boolean approved;
}
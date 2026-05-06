package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.mvp2.exchange.Exchange;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class Mvp2PaperHistoryService {

    private final List<Mvp2PaperTradeHistory> histories = new ArrayList<>();

    public synchronized void save(Mvp2PaperTradeHistory history) {
        histories.add(history);
    }

    public synchronized List<Mvp2PaperTradeHistory> recent(Exchange exchange, int limit) {
        return histories.stream()
                .filter(history -> history.exchange() == exchange)
                .skip(Math.max(0, histories.stream().filter(history -> history.exchange() == exchange).count() - limit))
                .toList();
    }
}

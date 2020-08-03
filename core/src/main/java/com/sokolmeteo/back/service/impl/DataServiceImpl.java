package com.sokolmeteo.back.service.impl;

import com.sokolmeteo.back.service.DataService;
import com.sokolmeteo.dao.model.Log;
import com.sokolmeteo.dao.model.WeatherData;
import com.sokolmeteo.dao.repo.LogDao;
import com.sokolmeteo.sokol.tcp.TcpClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataServiceImpl implements DataService {
    private final LogDao logDao;
    private final TcpClient tcpClient;

    public DataServiceImpl(LogDao logDao, TcpClient tcpClient) {
        this.logDao = logDao;
        this.tcpClient = tcpClient;
    }

    @Override
    public ResponseEntity<Long> sendData(MultipartFile file, String author) {
        Log log = new Log(author);
        log = logDao.save(log);
        if (!file.isEmpty()) {
            try {
                WeatherData data = processMessages(file);
                DataSenderService dataSender = new DataSenderService(logDao, tcpClient).setData(data).setLog(log);
                dataSender.start();
                return new ResponseEntity<>(log.getId(), HttpStatus.OK);
            } catch (IOException e) {
                log.fault(e.getMessage(), null);
                logDao.save(log);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        log.fault("File is empty", null);
        logDao.save(log);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Log> getState(Long dataId, String author) {
        Log log = logDao.findByIdAndAuthor(dataId, author);
        return log != null ? new ResponseEntity<>(log, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private WeatherData processMessages(MultipartFile file) throws IOException {
        WeatherData data = new WeatherData();
        try (InputStream inputStream = file.getInputStream()){
            List<String> lines =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.toList());
            data.setLoginMessage(lines.get(0) + "\r\n");
            List<String> payloads = new ArrayList<>();
            StringBuilder concatenated = new StringBuilder(lines.get(1));
            for (String payload : lines.subList(2, lines.size())) {
                if (concatenated.length() > 3500) {
                    concatenated.append("\r\n");
                    payloads.add(concatenated.toString());
                    concatenated = new StringBuilder(lines.get(1));
                }
                concatenated.append(payload);
            }
            payloads.add(concatenated.append("\r\n").toString());
            data.setBlackMessages(payloads);
        }
        return data;
    }
}
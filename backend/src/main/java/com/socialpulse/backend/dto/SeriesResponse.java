package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;

import java.util.List;

public record SeriesResponse(Platform platform, Metric metric, Granularity granularity, List<SeriesPointDto> points) {}

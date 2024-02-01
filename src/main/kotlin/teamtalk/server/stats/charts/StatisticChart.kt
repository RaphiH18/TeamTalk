package teamtalk.server.stats.charts

import javafx.scene.chart.Chart

abstract class StatisticChart {

    abstract fun create(): Chart

    abstract fun update()

    abstract fun getChart(): Chart

}
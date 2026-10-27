// ECharts 按需注册 —— 取代全量 `import * as echarts from 'echarts'`
// 收益：主 bundle 减少 ~800KB（砍掉未使用的 tree/treemap/sankey/sunburst/parallel/candlestick/boxplot 等图表）
// 添加新图表时，在这里 import + 加入 use 数组即可。
import * as echarts from 'echarts/core'
import {
  BarChart, LineChart, PieChart, ScatterChart, EffectScatterChart, HeatmapChart, GaugeChart, GraphChart
} from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  DatasetComponent, TransformComponent, ToolboxComponent,
  DataZoomComponent, MarkLineComponent, MarkPointComponent, MarkAreaComponent,
  VisualMapComponent, GeoComponent, GraphicComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  // 图表类型
  BarChart, LineChart, PieChart, ScatterChart, EffectScatterChart, HeatmapChart, GaugeChart, GraphChart,
  // 组件
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  DatasetComponent, TransformComponent, ToolboxComponent,
  DataZoomComponent, MarkLineComponent, MarkPointComponent, MarkAreaComponent,
  VisualMapComponent, GeoComponent, GraphicComponent,
  // 渲染器
  CanvasRenderer
])

export default echarts

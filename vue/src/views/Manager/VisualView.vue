<template>
  <div class="visualization-container">
    <h2>可视化展示</h2>
    <!-- 顶部工具栏 -->
    <div class="toolbar">

      <el-input
          v-model="pid"
          placeholder="请输入项目ID (pid)"
          clearable
          style="width: 400px; margin-right: 10px;"
      />
      <el-button type="primary" @click="loadData">加载可视化数据</el-button>
      <span v-show="loading" class="loading-text">正在加载中...</span>
    </div>

    <!-- 图表区域 -->
    <div class="grid-container">
      <!-- 第一行：情感分析饼图 + 舆情趋势折线图 -->
      <el-card class="chart-card">
        <div class="card-header">
          <span>情感分析结果分布（饼图）</span>
          <el-button type="text" size="small" @click="exportChart('sentiment')">📸 导出图片</el-button>
        </div>
        <div ref="sentimentEl" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <div class="card-header">
          <span>舆情热度变化趋势（折线图）</span>
          <el-button type="text" size="small" @click="exportChart('trend')">📸 导出图片</el-button>
        </div>
        <div ref="trendEl" class="chart-box"></div>
      </el-card>

      <!-- 第二行：关键词词云 + 情感柱状图 -->
      <el-card class="chart-card">
        <div class="card-header">
          <span>关键词词云分析</span>
          <el-button type="text" size="small" @click="exportChart('word')">📸 导出图片</el-button>
        </div>
        <div ref="wordEl" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <div class="card-header">
          <span>情感数量对比（柱状图）</span>
          <el-button type="text" size="small" @click="exportChart('bar')">📸 导出图片</el-button>
        </div>
        <div ref="barEl" class="chart-box"></div>
      </el-card>
    </div>

    <!-- 第三部分：社交网络图（不改动！） -->
    <el-card class="chart-card full-width">
      <div slot="header" class="card-header">
        <span>社交网络节点关系图</span>
        <el-button type="text" size="small" @click="exportChart('graph')">📸 导出图片</el-button>
      </div>
      <div ref="graphEl" style="width:100%; height:700px;"></div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from "echarts";
import "echarts-wordcloud";
import request from "@/utils/requests";

export default {
  name: "VisualizationView",
  data() {
    return {
      pid: "",
      loading: false,
      charts: {
        sentiment: null,
        trend: null,
        word: null,
        bar: null,
        graph: null,
      },
    };
  },
  beforeDestroy() {
    Object.values(this.charts).forEach((c) => c?.dispose?.());
  },
  methods: {
    async loadData() {
      if (!this.pid) {
        this.$message.warning("请先输入项目ID（pid）");
        return;
      }
      this.loading = true;
      try {
        const unwrapObj = (res) => (res && (res.data ?? res)) || {};
        const unwrapArr = (res) => {
          const raw = res && (res.data ?? res);
          return Array.isArray(raw) ? raw : [];
        };

        const [sentimentRes, trendRes, keywordRes, graphRes] = await Promise.all([
          request.get(`/visual/sentiment?pid=${this.pid}`),
          request.get(`/visual/trend?pid=${this.pid}`),
          request.get(`/visual/keywords?pid=${this.pid}`),
          request.get(`/graph/project/${this.pid}`),
        ]);


        const sentiment = unwrapObj(sentimentRes);          // 期望得到 {positive, neutral, negative}
        const trend     = unwrapArr(trendRes);              // 期望得到 [{date, comments, likes}]
        const keywords  = unwrapArr(keywordRes);            // 期望得到 [{word, count}]
        const graphData = graphRes?.data?.data || {};

        this.loading = false;
        await this.$nextTick();
        window.dispatchEvent(new Event('resize'));

        this.drawSentiment(sentiment);
        this.drawTrend(trend);
        this.drawWordCloud(keywords);
        this.drawSentimentBar(sentiment);
        this.renderGraph(graphData);

        window.dispatchEvent(new Event("resize"));

      } catch (err) {
        console.error("❌ 加载失败:", err);
        this.$message.error("加载失败，请检查后端接口");
        this.loading = false;
      }
    },

    /** 工具：保证DOM存在后初始化ECharts实例 */
    ensureChart(refName, key) {
      const el = this.$refs[refName];
      if (!el) return null;
      if (this.charts[key] && !this.charts[key].isDisposed()) return this.charts[key];
      this.charts[key] = echarts.init(el);
      window.addEventListener("resize", () => this.charts[key]?.resize());
      return this.charts[key];
    },

    /** 导出当前图表为图片 */
    exportChart(key) {
      const chart = this.charts[key];
      if (!chart) {
        this.$message.warning("请先加载数据再导出图片");
        return;
      }
      const img = chart.getDataURL({
        type: "png",
        pixelRatio: 2,
        backgroundColor: "#fff",
      });
      const a = document.createElement("a");
      a.href = img;
      a.download = `${key}_chart.png`;
      a.click();
    },

    /** 1️⃣ 情感分析饼图 */
    drawSentiment(data) {
      const chart = this.ensureChart("sentimentEl", "sentiment");
      chart.clear();
      chart.hideLoading();
      console.log("🎯 Sentiment Data:", data);

      const option = {
        title: { text: "情感分布", left: "center" },
        tooltip: {
          trigger: "item",
          formatter: "{b}：{c}（{d}%）"},
        series: [
          {
            name: "情感类别",
            type: "pie",
            label: {
              show: true,
              position: "outside",
              formatter: "{b}\n{d}%"
            },
            data: [
              { value: data.positive || 0, name: "正面" },
              { value: data.neutral || 0, name: "中性" },
              { value: data.negative || 0, name: "负面" },
            ],
          },
        ],
      };
      chart.setOption(option, true);
    },

    /** 2️⃣ 舆情热度变化趋势折线图 */
    drawTrend(data) {
      const chart = this.ensureChart("trendEl", "trend");
      const dates = data.map(d => {
        const t = Number(d.date);           // 兼容字符串/数字
        return isNaN(t) ? d.date : new Date(t).toLocaleDateString('zh-CN');
      });
      const comments = data.map((d) => d.comments || 0);
      const likes = data.map((d) => d.likes || 0);
      const option = {
        title: { text: "舆情趋势", left: "center" },
        tooltip: { trigger: "axis" },
        legend: { data: ["评论数", "点赞数"], bottom: 0 },
        xAxis: { type: "category", data: dates, boundaryGap: false },
        yAxis: { type: "value" },
        series: [
          { name: "评论数", type: "line", data: comments, smooth: true },
          { name: "点赞数", type: "line", data: likes, smooth: true },
        ],
      };
      chart.setOption(option, true);
    },

    /** 3️⃣ 关键词词云 */
    drawWordCloud(data) {
      const chart = this.ensureChart("wordEl", "word");
      const option = {
        title: { text: "关键词词云", left: "center" },
        series: [
          {
            type: "wordCloud",
            shape: "circle",
            width: "100%",
            height: "100%",
            sizeRange: [12, 50],
            rotationRange: [0, 0],
            gridSize: 4,
            textStyle: {
              color: () =>
                  `rgb(${Math.random() * 200},${Math.random() * 200},${Math.random() * 200})`,
            },
            data: (data || []).map((item) => ({ name: item.word, value: item.count })),
          },
        ],
      };
      chart.setOption(option, true);
    },

    /** 4️⃣ 情感数量柱状图 */
    drawSentimentBar(data) {
      const chart = this.ensureChart("barEl", "bar");
      chart.clear();
      chart.hideLoading();
      const option = {
        title: { text: "情感数量对比", left: "center" },
        tooltip: {},
        xAxis: {
          type: "category",
          data: ["正面", "中性", "负面"],
        },
        yAxis: { type: "value" },
        series: [
          {
            data: [data.positive || 0, data.neutral || 0, data.negative || 0],
            type: "bar",
            barWidth: "40%",
            itemStyle: {
              color: (params) => {
                const colors = ["#4CAF50", "#FFC107", "#F44336"];
                return colors[params.dataIndex];
              },
            },
          },
        ],
      };
      chart.setOption(option, true);
      chart.resize();
    },

    /** ✅ 5️⃣ 社交网络图（保持原样） */
    renderGraph(graphData) {
      const chart = this.ensureChart("graphEl", "graph");
      if (!graphData?.nodes || !graphData?.links) {
        chart.clear();
        chart.showLoading({ text: "暂无社交网络数据" });
        return;
      }

      /** 工具函数：标准化社区名 */
      const communityNameOf = (val) => {
        if (val === null || val === undefined || val === "") return "社区 -";
        return `社区 ${isNaN(val) ? String(val) : Number(val)}`;
      };

      /** 工具函数：排序社区 */
      const sortCommunityNames = (names) => {
        return names.slice().sort((a, b) => {
          const na = Number(a.replace("社区 ", ""));
          const nb = Number(b.replace("社区 ", ""));
          const aIsNum = !isNaN(na);
          const bIsNum = !isNaN(nb);
          if (aIsNum && bIsNum) return na - nb;
          if (aIsNum && !bIsNum) return -1;
          if (!aIsNum && bIsNum) return 1;
          return a.localeCompare(b, "zh-Hans-CN-u-nu-hanidec");
        });
      };

      // === 1️⃣ 社区名有序化 ===
      const allNames = graphData.nodes.map((n) => communityNameOf(n.community));
      const sortedNames = sortCommunityNames(Array.from(new Set(allNames)));

      const categories = sortedNames.map((name) => ({ name }));
      const nameToIndex = new Map(categories.map((c, i) => [c.name, i]));

      // === 2️⃣ 节点数据 ===
      const nodes = graphData.nodes.map((n) => {
        const cname = communityNameOf(n.community);
        const catIndex = nameToIndex.get(cname) ?? 0;
        return {
          id: n.id,
          name: n.name || n.id,
          symbolSize: 15 + (n.pagerank || 0.01) * 600,
          value: n.pagerank || 0.01,
          category: catIndex,
          communityName: cname,
          in_degree: n.in_degree || 0,
          out_degree: n.out_degree || 0,
          pagerank: n.pagerank || 0,
          content: n.content || "(无评论内容)",
          label: { show: (n.pagerank || 0) > 0.012 },
        };
      });

      // === 3️⃣ 连线数据 ===
      const links = graphData.links.map((link) => ({
        source: link.source,
        target: link.target,
        lineStyle: {
          width: Math.min(1 + (link.weight || 1) * 0.3, 3),
          color: "#aaa",
        },
      }));

      // === 4️⃣ 图配置 ===
      const option = {
        backgroundColor: "#fff",
        tooltip: {
          show: true,
          borderWidth: 0.5,
          backgroundColor: "rgba(255,255,255,0.95)",
          textStyle: { color: "#333", fontSize: 12 },
          formatter: (params) => {
            if (params.dataType === "node") {
              const node = params.data;
              return `
            <b>${node.name}</b><br/>
            ${node.communityName}<br/>
            入度：${node.in_degree}<br/>
            出度：${node.out_degree}<br/>
            PageRank：${Number(node.pagerank || 0).toFixed(4)}<br/>
            <hr style="margin:4px 0;"/>
            <div style="color:#555;">${node.content}</div>
          `;
            }
            return `${params.data.source} → ${params.data.target}`;
          },
        },
        legend: {
          data: categories.map((c) => c.name), // ✅ 顺序一致
          orient: "vertical",
          right: 10,
          top: 20,
          textStyle: { color: "#333", fontSize: 12 },
          selectedMode: "multiple",
        },
        series: [
          {
            name: "用户互动图",
            type: "graph",
            layout: "force",
            data: nodes,
            links: links,
            categories: categories,
            roam: true,
            draggable: true,
            focusNodeAdjacency: true,
            label: { position: "right", color: "#222", fontSize: 10 },
            lineStyle: { opacity: 0.7, width: 1 },
            force: { repulsion: 200, edgeLength: [50, 150], gravity: 0.2 },
            emphasis: { focus: "adjacency", lineStyle: { width: 2 } },
          },
        ],
      };

      chart.clear();
      chart.setOption(option, true);
    }
  },
};
</script>

<style scoped>
.visualization-container {
  padding: 20px;
  background-color: #f9f9f9;
}
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.loading-text {
  margin-left: 12px;
  color: #888;
}

/* ✅ 两行两列网格布局 */
.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 24px;
  margin-bottom: 30px;
}
.chart-card {
  width: 100%;
}
.chart-box {
  width: 100%;
  height: 400px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  font-size: 14px;
}
.full-width {
  margin-top: 20px;
}

</style>

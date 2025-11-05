<template>
  <div class="network-container">
    <h2>社交网络</h2>
    <div class="toolbar">
      <el-input
          v-model="pid"
          placeholder="请输入项目ID (pid)"
          clearable
          style="width: 400px; margin-right: 10px;"
      ></el-input>
      <el-button type="primary" @click="loadGraphData">加载图数据</el-button>
    </div>

    <div v-if="loading" class="loading">正在加载中，请稍候...</div>
    <div id="graphContainer" style="width: 100%; height: 80vh;"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";
import request from "@/utils/requests";

export default {
  name: "NetworkView",
  data() {
    return {
      pid: "",
      loading: false,
      chart: null,
      // 原始基线数据（不改它，只在视图需要时复制）
      _baseCategories: [],
      _baseNodes: [],
      _baseLinks: [],
      _baseSeries: null,
      _activeCommunity: null,
    };
  },
  methods: {
    async loadGraphData() {
      if (!this.pid) {
        this.$message.warning("请先输入项目ID（pid）");
        return;
      }
      this.loading = true;
      try {
        await request.post(`/graph/build/${this.pid}`);
        const res = await request.get(`/graph/project/${this.pid}`);
        const graphData = res.data?.data?.data || res.data?.data;
        if (!graphData || !graphData.nodes) {
          this.$message.error("未获取到图数据，请检查项目ID或后端接口");
          return;
        }
        this.renderGraph(graphData);
      } catch (err) {
        console.error(err);
        this.$message.error("加载失败，请检查网络或接口配置");
      } finally {
        this.loading = false;
      }
    },

    /** “社区”显示名 */
    communityNameOf(val) {
      if (val === null || val === undefined || val === "") return "社区 -";
      return `社区 ${isNaN(val) ? String(val) : Number(val)}`;
    },

    /** 稳定排序：数字在前，非数字在后；同类再按字典序 */
    sortCommunityNames(names) {
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
    },

    renderGraph(graphData) {
      if (!this.chart) {
        this.chart = echarts.init(document.getElementById("graphContainer"));
        window.addEventListener("resize", () => this.chart.resize());
      }

      // 1) 规范化 & 有序的社区名
      const allNames = graphData.nodes.map((n) => this.communityNameOf(n.community));
      const sortedNames = this.sortCommunityNames(Array.from(new Set(allNames)));

      // categories 用“名称”保证 legend 与 series 对齐
      const categories = sortedNames.map((name) => ({ name }));
      const nameToIndex = new Map(categories.map((c, i) => [c.name, i]));

      // 2) 节点（category 必须是索引，但来源于名称映射，避免错位）
      const nodes = graphData.nodes.map((n) => {
        const cname = this.communityNameOf(n.community);
        const catIndex = nameToIndex.get(cname) ?? 0;
        return {
          id: n.id,
          name: n.name || n.id,
          symbolSize: 12 + (n.pagerank || 0) * 800,
          value: n.pagerank || 0,
          category: catIndex,           // ★ 与 legend/categories 对齐
          communityName: cname,         // 显示用
          in_degree: n.in_degree ?? 0,
          out_degree: n.out_degree ?? 0,
          pagerank: n.pagerank ?? 0,
          content: n.content || "(无评论内容)",
          label: { show: (n.pagerank || 0) > 0.01 },
        };
      });

      // 3) 连线
      const links = graphData.links.map((l) => ({
        source: l.source,
        target: l.target,
        lineStyle: {
          width: Math.min(1 + (l.weight || 1) * 0.2, 3),
          color: "rgba(150,150,150,0.45)",
        },
      }));

      // 4) 基础配置（legend.data 与 categories 名称严格一致 + 关闭悬停联动）
      const baseSeries = {
        name: "用户互动网络",
        type: "graph",
        layout: "force",
        data: nodes,
        links: links,
        categories: categories,
        roam: true,
        draggable: true,
        focusNodeAdjacency: true,
        legendHoverLink: false, // ← 禁用“悬停联动”，避免错位
        label: { position: "right", color: "#222", fontSize: 10 },
        lineStyle: { opacity: 0.6, width: 1 },
        force: { repulsion: 220, edgeLength: [60, 160], gravity: 0.15 },
        emphasis: { focus: "adjacency", lineStyle: { width: 2 } },
      };

      const option = {
        backgroundColor: "#f8f9fb",
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
          // ★ 用 series 的 categories 作为 legend 数据源，同时显式指定 data，确保顺序一致
          data: categories.map((c) => c.name),
          orient: "vertical",
          right: 15,
          top: 20,
          textStyle: { color: "#333", fontSize: 12 },
          selectedMode: "multiple",
        },
        series: [baseSeries],
      };

      // 5) 渲染
      this.chart.setOption(option, true);

      // 6) 缓存 baseline，后续点击图例时按需生成新数据
      this._baseCategories = categories;
      this._baseNodes = nodes;
      this._baseLinks = links;
      this._baseSeries = baseSeries;
      this._activeCommunity = null;

      // —— 只用“点击选择”来控制（官方支持）——
      this.chart.off("legendselectchanged");
      this.chart.on("legendselectchanged", (e) => {
        // e.selected 是一个 { '社区 X': boolean } 的映射
        const selectedNames = Object.keys(e.selected).filter((k) => e.selected[k]);

        // 0 个或全选：恢复基础样式（不做灰度）
        if (selectedNames.length === 0 || selectedNames.length === this._baseCategories.length) {
          this._activeCommunity = null;
          this.chart.setOption({
            series: [{ ...this._baseSeries, data: this._baseNodes, links: this._baseLinks }],
          });
          return;
        }

        // 仅当“只剩 1 个被选中”时，做轻量强化（不会把全图变灰；其余类别已被 legend 隐藏）
        if (selectedNames.length === 1) {
          const name = selectedNames[0];
          this._activeCommunity = name;

          // 轻量强化：目标社区节点放大/着色；边对两端可见节点做强调
          const sizedNodes = this._baseNodes.map((n) =>
              n.communityName === name
                  ? { ...n, symbolSize: n.symbolSize * 1.2, itemStyle: { color: "#ff6666" } }
                  : { ...n }
          );

          const styledLinks = this._baseLinks.map((l) => {
            const s = this._baseNodes.find((n) => n.name === l.source);
            const t = this._baseNodes.find((n) => n.name === l.target);
            const hit =
                s?.communityName === name || t?.communityName === name;
            return {
              ...l,
              lineStyle: {
                ...l.lineStyle,
                color: hit ? "#ff6666" : l.lineStyle.color,
                width: hit ? 2 : l.lineStyle.width,
                opacity: hit ? 0.9 : l.lineStyle.opacity,
              },
            };
          });

          this.chart.setOption({
            series: [{ ...this._baseSeries, data: sizedNodes, links: styledLinks }],
          });
          return;
        }

        // 其余情况（≥2 个被选中）：不做任何滤镜，交给 ECharts 自己显示
        this._activeCommunity = null;
        this.chart.setOption({
          series: [{ ...this._baseSeries, data: this._baseNodes, links: this._baseLinks }],
        });
      });
    },
  },
};
</script>

<style scoped>
.network-container {
  padding: 20px;
  background-color: #f9f9f9;
}
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.loading {
  text-align: center;
  font-size: 16px;
  margin-top: 20px;
  color: #666;
}
</style>

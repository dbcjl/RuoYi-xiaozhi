<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="智能体" prop="agentName">
        <el-input
          v-model="queryParams.agentName"
          placeholder="请输入智能体名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="语音合成" prop="ttsProvider">
        <el-select v-model="queryParams.ttsProvider" placeholder="请选择TTS模型" clearable style="width: 180px">
          <el-option
              v-for="dict in tts_provider"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['intellect:agent:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['intellect:agent:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['intellect:agent:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['intellect:agent:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="agentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="智能体ID" align="center" prop="id" min-width="175" />
      <el-table-column label="智能体" align="center" prop="agentName" min-width="100" show-overflow-tooltip />
      <el-table-column label="语音合成" align="center" prop="ttsProvider" min-width="120">
        <template #default="scope">
          <dict-tag :options="tts_provider" :value="scope.row.ttsProvider" />
        </template>
      </el-table-column>
      <el-table-column label="提示词" align="center" prop="prompt" min-width="200" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['intellect:agent:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['intellect:agent:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改智能体对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="agentRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="agentName">
          <el-input
              v-model="form.agentName"
              placeholder="请输入智能体名称"
              prefix-icon="Tickets"
              maxlength="30"
              show-word-limit
          />
        </el-form-item>
        <el-form-item label="语音合成" prop="ttsProvider">
          <el-select v-model="form.ttsProvider" placeholder="请选择TTS模型">
            <el-option
                v-for="dict in tts_provider"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提示词" prop="prompt">
          <el-input
              v-model="form.prompt"
              type="textarea"
              placeholder="请输入角色提示词"
              maxlength="500"
              rows="15"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Agent">
import { listAgent, getAgent, delAgent, addAgent, updateAgent } from "@/api/intellect/agent"

const { proxy } = getCurrentInstance()
const { tts_provider } = proxy.useDict("tts_provider")

const agentList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    agentName: null,
    prompt: null,
    ttsProvider: null,
  },
  rules: {
    agentName: { required: true, message: "智能体名称不能为空", trigger: "blur" },
    ttsProvider: { required: true, message: "请选择语音合成模型", trigger: "blur" },
    prompt: { required: true, message: "系统提示词不能为空", trigger: "blur" }
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询智能体列表 */
function getList() {
  loading.value = true
  listAgent(queryParams.value).then(response => {
    agentList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    agentName: null,
    prompt: null,
    ttsProvider: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm("agentRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加智能体"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAgent(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改智能体"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["agentRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAgent(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAgent(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除智能体编号为"' + _ids + '"的数据项？').then(function() {
    return delAgent(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('intellect/agent/export', {
    ...queryParams.value
  }, `agent_${new Date().getTime()}.xlsx`)
}

getList()
</script>

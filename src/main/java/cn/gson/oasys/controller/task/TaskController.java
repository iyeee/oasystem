package cn.gson.oasys.controller.task;

import java.text.ParseException;


import java.util.Date;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;


import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.github.pagehelper.util.StringUtil;

import cn.gson.oasys.model.dao.roledao.RoleDao;
import cn.gson.oasys.model.dao.system.StatusDao;
import cn.gson.oasys.model.dao.system.TypeDao;
import cn.gson.oasys.model.dao.taskdao.TaskDao;
import cn.gson.oasys.model.dao.taskdao.TaskloggerDao;
import cn.gson.oasys.model.dao.taskdao.TaskuserDao;
import cn.gson.oasys.model.dao.user.DeptDao;
import cn.gson.oasys.model.dao.user.PositionDao;
import cn.gson.oasys.model.dao.user.UserDao;
import cn.gson.oasys.model.entity.role.Role;
import cn.gson.oasys.model.entity.system.SystemStatusList;
import cn.gson.oasys.model.entity.system.SystemTypeList;
import cn.gson.oasys.model.entity.task.Tasklist;
import cn.gson.oasys.model.entity.task.Tasklogger;
import cn.gson.oasys.model.entity.task.Taskuser;
import cn.gson.oasys.model.entity.user.Dept;
import cn.gson.oasys.model.entity.user.Position;
import cn.gson.oasys.model.entity.user.User;
import cn.gson.oasys.services.task.TaskService;

@Controller
@RequestMapping("/")
public class TaskController {

    @Autowired
    private TaskDao tdao;
    @Autowired
    private StatusDao sdao;
    @Autowired
    private TypeDao tydao;
    @Autowired
    private UserDao udao;
    @Autowired
    private DeptDao ddao;
    @Autowired
    private TaskuserDao tudao;
    @Autowired
    private TaskService tservice;
    @Autowired
    private TaskloggerDao tldao;
    @Autowired
    private PositionDao pdao;

    /**
     * ??????????????????
     *
     * @return
     */
    @RequestMapping("taskmanage")
    public String index(Model model,
                        @SessionAttribute("userId") Long userId,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size) {

        // ???????????????id?????????
        User tu = udao.findOne(userId);
        // ???????????????id????????????
        Page<Tasklist> tasklist = tservice.index(page, size, null, tu);
        List<Map<String, Object>> list = tservice.index2(tasklist, tu);

        model.addAttribute("tasklist", list);
        model.addAttribute("page", tasklist);
        model.addAttribute("url", "paixu");
        return "task/taskmanage";
    }

    /**
     * ????????????
     */
    @RequestMapping("paixu")
    public String paixu(HttpServletRequest request,
                        @SessionAttribute("userId") Long userId, Model model,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size) {

        // ???????????????id?????????
        User tu = udao.findOne(userId);
        String val = null;
        if (!StringUtil.isEmpty(request.getParameter("val"))) {
            val = request.getParameter("val").trim();
            model.addAttribute("sort", "&val=" + val);
        }

        Page<Tasklist> tasklist = tservice.index(page, size, val, tu);
        List<Map<String, Object>> list = tservice.index2(tasklist, tu);
        model.addAttribute("tasklist", list);
        model.addAttribute("page", tasklist);
        model.addAttribute("url", "paixu");

        return "task/managetable";

    }


    /**
     * ??????????????????
     */
    @RequestMapping("addtask")
    public ModelAndView index2(@SessionAttribute("userId") Long userId,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pa = new PageRequest(page, size);
        ModelAndView mav = new ModelAndView("task/addtask");
        // ???????????????
        Iterable<SystemTypeList> typelist = tydao.findAll();
        // ???????????????
        Iterable<SystemStatusList> statuslist = sdao.findAll();
        // ???????????????????????????
        Page<User> pagelist = udao.findByFatherId(userId, pa);
        List<User> emplist = pagelist.getContent();
        // ???????????????
        Iterable<Dept> deptlist = ddao.findAll();
        // ????????????
        Iterable<Position> poslist = pdao.findAll();
        mav.addObject("typelist", typelist);
        mav.addObject("statuslist", statuslist);
        mav.addObject("emplist", emplist);
        mav.addObject("deptlist", deptlist);
        mav.addObject("poslist", poslist);
        mav.addObject("page", pagelist);
        mav.addObject("url", "names");
        mav.addObject("qufen", "??????");
        return mav;
    }

    /**
     * ??????????????????
     */
    @RequestMapping("addtasks")
    public String addtask(@SessionAttribute("userId") Long userId, HttpServletRequest request) {
        User userlist = udao.findOne(userId);
        Tasklist list = (Tasklist) request.getAttribute("tasklist");
        request.getAttribute("success");
        list.setUsersId(userlist);
        list.setPublishTime(new Date());
        list.setModifyTime(new Date());
        tdao.save(list);
        // ?????????????????????
        StringTokenizer st = new StringTokenizer(list.getReciverlist(), ";");
        while (st.hasMoreElements()) {
            User reciver = udao.findid(st.nextToken());
            Taskuser task = new Taskuser();
            task.setTaskId(list);
            task.setUserId(reciver);
            task.setStatusId(list.getStatusId());
            // ??????????????????
            tudao.save(task);

        }

        return "redirect:/taskmanage";
    }

    /**
     * ????????????
     */
    @RequestMapping("edittasks")
    public ModelAndView index3(HttpServletRequest req, @SessionAttribute("userId") Long userId,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pa = new PageRequest(page, size);
        ModelAndView mav = new ModelAndView("task/edittask");
        // ????????????????????????id
        String taskid = req.getParameter("id");
        Long ltaskid = Long.parseLong(taskid);
        // ????????????id?????????????????????
        Tasklist task = tdao.findOne(ltaskid);
        // ????????????id
        Long statusid = task.getStatusId().longValue();
        // ????????????id
        Long typeid = task.getTypeId();
        // ???????????????
        SystemStatusList status = sdao.findOne(statusid);
        // ???????????????
        SystemTypeList type = tydao.findOne(typeid);

        // ???????????????????????????
        Page<User> pagelist = udao.findByFatherId(userId, pa);
        List<User> emplist = pagelist.getContent();

        // ???????????????
        Iterable<Dept> deptlist = ddao.findAll();
        // ????????????
        Iterable<Position> poslist = pdao.findAll();
        mav.addObject("type", type);
        mav.addObject("status", status);
        mav.addObject("emplist", emplist);
        mav.addObject("deptlist", deptlist);
        mav.addObject("poslist", poslist);
        mav.addObject("task", task);
        mav.addObject("page", pagelist);
        mav.addObject("url", "names");
        mav.addObject("qufen", "??????");
        return mav;
    }

    /**
     * ??????????????????
     */
    @RequestMapping("update")
    public String update(Tasklist task, HttpSession session) {
        String userId = (session.getAttribute("userId").toString()).trim();
        Long userid = Long.parseLong(userId);
        User userlist = udao.findOne(userid);
        task.setUsersId(userlist);
        task.setPublishTime(new Date());
        task.setModifyTime(new Date());
        tservice.save(task);

        // ????????????????????? ??????????????????????????????
        StringTokenizer st = new StringTokenizer(task.getReciverlist(), ";");

        while (st.hasMoreElements()) {
            String tmp=st.nextToken();
            if (tmp == null) {
                 tmp = task.getReciverlist();
            }
            System.out.println(tmp);
            User reciver = udao.findid(tmp);
            Long pkid = udao.findpkId(task.getTaskId(), reciver.getUserId());
            Taskuser tasku = new Taskuser();
            tasku.setPkId(pkid);
            tasku.setTaskId(task);
            tasku.setUserId(reciver);
            tasku.setStatusId(task.getStatusId());
            // ??????????????????
            tudao.save(tasku);

        }

        return "redirect:/taskmanage";

    }

    /**
     * ????????????
     */
    @RequestMapping("seetasks")
    public ModelAndView index4(HttpServletRequest req) {
        ModelAndView mav = new ModelAndView("task/seetask");
        // ??????????????? id
        String taskid = req.getParameter("id");
        Long ltaskid = Long.parseLong(taskid);
        // ????????????id?????????????????????
        Tasklist task = tdao.findOne(ltaskid);
        Long statusid = task.getStatusId().longValue();

        // ????????????id???????????????
        SystemStatusList status = sdao.findOne(statusid);
        // ???????????????
        Iterable<SystemStatusList> statuslist = sdao.findAll();
        // ???????????????
        User user = udao.findOne(task.getUsersId().getUserId());
        // ?????????????????????
        List<Tasklogger> logger = tldao.findByTaskId(ltaskid);
        mav.addObject("task", task);
        mav.addObject("user", user);
        mav.addObject("status", status);
        mav.addObject("loggerlist", logger);
        mav.addObject("statuslist", statuslist);
        return mav;
    }

    /**
     * ???????????????
     *
     * @return
     */
    @RequestMapping("tasklogger")
    public String tasklogger(Tasklogger logger, @SessionAttribute("userId") Long userId) {
        User userlist = udao.findOne(userId);
        logger.setCreateTime(new Date());
        logger.setUsername(userlist.getUserName());
        // ?????????
        tldao.save(logger);
        // ??????????????????
        tservice.updateStatusid(logger.getTaskId().getTaskId(), logger.getLoggerStatusid());
        // ???????????????????????????
        tservice.updateUStatusid(logger.getTaskId().getTaskId(), logger.getLoggerStatusid());

        return "redirect:/taskmanage";

    }

    /**
     * ????????????
     */
    @RequestMapping("mytask")
    public String index5(@SessionAttribute("userId") Long userId, Model model,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pa = new PageRequest(page, size);
        Page<Tasklist> tasklist = tservice.index3(userId, null, page, size);

        Page<Tasklist> tasklist2 = tdao.findByTickingIsNotNull(pa);
        if (tasklist != null) {
            List<Map<String, Object>> list = tservice.index4(tasklist, userId);
            model.addAttribute("page", tasklist);
            model.addAttribute("tasklist", list);
        } else {
            List<Map<String, Object>> list2 = tservice.index4(tasklist2, userId);
            model.addAttribute("page", tasklist2);
            model.addAttribute("tasklist", list2);
        }
        model.addAttribute("url", "mychaxun");
        return "task/mytask";

    }

    /**
     * ?????????????????????????????????
     *
     * @throws ParseException
     */
    @RequestMapping("mychaxun")
    public String select(HttpServletRequest request, @SessionAttribute("userId") Long userId, Model model,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "size", defaultValue = "10") int size) throws ParseException {

        String title = null;
        if (!StringUtil.isEmpty(request.getParameter("title"))) {
            title = request.getParameter("title").trim();
        }
        Page<Tasklist> tasklist = tservice.index3(userId, title, page, size);
        List<Map<String, Object>> list = tservice.index4(tasklist, userId);
        model.addAttribute("tasklist", list);
        model.addAttribute("page", tasklist);
        model.addAttribute("url", "mychaxun");
        model.addAttribute("sort", "&title=" + title);
        return "task/mytasklist";
    }


    @RequestMapping("myseetasks")
    public ModelAndView myseetask(HttpServletRequest req, @SessionAttribute("userId") Long userId) {

        ModelAndView mav = new ModelAndView("task/myseetask");
        // ??????????????? id
        String taskid = req.getParameter("id");

        Long ltaskid = Long.parseLong(taskid);
        // ????????????id?????????????????????
        Tasklist task = tdao.findOne(ltaskid);

        // ???????????????
        Iterable<SystemStatusList> statuslist = sdao.findAll();
        // ??????????????????????????????
        Long ustatus = tudao.findByuserIdAndTaskId(userId, ltaskid);

        SystemStatusList status = sdao.findOne(ustatus);
        /*System.out.println(status);*/

        // ???????????????
        User user = udao.findOne(task.getUsersId().getUserId());
        // ?????????????????????
        List<Tasklogger> logger = tldao.findByTaskId(ltaskid);

        mav.addObject("task", task);
        mav.addObject("user", user);
        mav.addObject("status", status);
        mav.addObject("statuslist", statuslist);
        mav.addObject("loggerlist", logger);
        return mav;

    }

    /**
     * ????????????????????????????????????????????????
     */
    @RequestMapping("uplogger")
    public String updatelo(Tasklogger logger, @SessionAttribute("userId") Long userId) {
        System.out.println(logger.getLoggerStatusid());
        // ????????????id

        // ????????????
        User user = udao.findOne(userId);
        // ?????????
        Tasklist task = tdao.findOne(logger.getTaskId().getTaskId());
        logger.setCreateTime(new Date());
        logger.setUsername(user.getUserName());
        // ?????????
        tldao.save(logger);

        // ???????????????????????????
        Long pkid = udao.findpkId(logger.getTaskId().getTaskId(), userId);
        Taskuser tasku = new Taskuser();
        tasku.setPkId(pkid);
        tasku.setTaskId(task);
        tasku.setUserId(user);
        if (!Objects.isNull(logger.getLoggerStatusid())) {

            tasku.setStatusId(logger.getLoggerStatusid());
        }
        // ??????????????????
        tudao.save(tasku);

        // ??????????????????
        // ????????????id???????????????

        List<Integer> statu = tudao.findByTaskId(logger.getTaskId().getTaskId());
        System.out.println(statu);
        // ?????????????????????id ??????????????????????????????
        Integer min = statu.get(0);
        for (Integer integer : statu) {
            if (integer.intValue() < min) {
                min = integer;
            }
        }

        int up = tservice.updateStatusid(logger.getTaskId().getTaskId(), min);
		/*System.out.println(logger.getTaskId().getTaskId() + "aaaa");
		System.out.println(min + "wwww");
		System.out.println(up + "pppppp");*/
        if (up > 0) {
            System.out.println("????????????????????????!");
        }

        return "redirect:/mytask";

    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param req
     * @return
     */
    @RequestMapping("shanchu")
    public String delete(HttpServletRequest req, @SessionAttribute("userId") Long userId) {
        // ??????????????? id
        String taskid = req.getParameter("id");
        Long ltaskid = Long.parseLong(taskid);

        // ????????????id??????????????????
        Tasklist task = tdao.findOne(ltaskid);
        if (task.getUsersId().getUserId().equals(userId)) {
            // ???????????????
            int i = tservice.detelelogger(ltaskid);
            System.out.println(i + "mmmmmmmmmmmm");
            // ????????????????????? ?????????????????????????????????????????????????????????
            StringTokenizer st = new StringTokenizer(task.getReciverlist(), ";");
            while (st.hasMoreElements()) {
                User reciver = udao.findid(st.nextToken());
                Long pkid = udao.findpkId(task.getTaskId(), reciver.getUserId());
                int m = tservice.delete(pkid);
                System.out.println(m + "sssssssssss");

            }
            // ??????????????????
            tservice.deteletask(task);
        } else {
            System.out.println("??????????????????????????????");
            return "redirect:/notlimit";

        }
        return "redirect:/taskmanage";

    }

    /**
     * ?????????????????????
     */
    @RequestMapping("myshanchu")
    public String mydelete(HttpServletRequest req, @SessionAttribute("userId") Long userId) {
        // ??????id
        // ??????????????? id
        String taskid = req.getParameter("id");
        Long ltaskid = Long.parseLong(taskid);
        Long pkid = udao.findpkId(ltaskid, userId);
        tservice.delete(pkid);

        return "redirect:/mytask";

    }


}

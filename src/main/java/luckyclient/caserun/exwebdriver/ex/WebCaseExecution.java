package luckyclient.caserun.exwebdriver.ex;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import luckyclient.caserun.exinterface.TestCaseExecution;
import luckyclient.caserun.exwebdriver.BaseWebDrive;
import luckyclient.caserun.exwebdriver.EncapsulateOperation;
import luckyclient.dblog.LogOperation;
import luckyclient.planapi.entity.ProjectCase;
import luckyclient.planapi.entity.ProjectCasesteps;
import luckyclient.planapi.entity.PublicCaseParams;
import luckyclient.publicclass.ChangString;

/**
 * =================================================================
 * 这是一个受限制的自由软件！您不能在任何未经允许的前提下对程序代码进行修改和用于商业用途；也不允许对程序代码修改后以任何形式任何目的的再发布。
 * 为了尊重作者的劳动成果，LuckyFrame关键版权信息严禁篡改
 * 有任何疑问欢迎联系作者讨论。 QQ:1573584944  seagull1985
 * =================================================================
 * 
 * @author： seagull
 * @date 2017年12月1日 上午9:29:40
 * 
 */
public class WebCaseExecution extends TestCaseExecution{
	static Map<String, String> variable = new HashMap<String, String>();

	public static void caseExcution(ProjectCase testcase, List<ProjectCasesteps> steps,String taskid, WebDriver wd,LogOperation caselog,List<PublicCaseParams> pcplist)
			throws InterruptedException {
		int setresult = 0; // 0:成功 1:失败 2:锁定 其他：锁定
		String casenote = "备注初始化";
		String imagname = "";
		// 把公共参数加入到MAP中
		for (PublicCaseParams pcp : pcplist) {
			variable.put(pcp.getParamsname(), pcp.getParamsvalue());
		}
		
		caselog.addCaseDetail(taskid, testcase.getSign(), "1", testcase.getName(), 4);       //插入开始执行的用例
		
		for (ProjectCasesteps step : steps) {
			Map<String, String> params = WebDriverAnalyticCase.analyticCaseStep(testcase, step, taskid,caselog);
			
			if(params.get("exception")!=null&&params.get("exception").toString().indexOf("解析异常")>-1){
				setresult = 2;
				break;
			}
			
			String result = WebCaseExecution.runStep(params, wd, taskid, testcase.getSign(), step.getStepnum(), caselog);

			String expectedResults = params.get("ExpectedResults").toString();
			expectedResults=ChangString.changparams(expectedResults, variable,"预期结果");

			if (result.indexOf("出错") < 0 && result.indexOf("失败") < 0) { // 运行结果正常
				int waitsec = Integer.parseInt(params.get("StepWait").toString()); // 获取步骤间等待时间
				if (waitsec != 0) {
					luckyclient.publicclass.LogUtil.APP.info("操作休眠【"+waitsec+"】秒");
					Thread.sleep(waitsec * 1000);
				}
				
				if (!"".equals(expectedResults)) { // 有预期结果
					// 判断传参
					luckyclient.publicclass.LogUtil.APP.info("expectedResults=【"+expectedResults+"】");
					if (expectedResults.length() > 2 && expectedResults.substring(0, 2).indexOf("$=") > -1) {
						String expectedResultVariable = expectedResults.substring(2);
						variable.put(expectedResultVariable, result);
						continue;
					}

					// 判断预期结果-检查模式
					if (params.get("checkproperty") != null && params.get("checkproperty_value") != null) {
						String checkproperty = params.get("checkproperty").toString();
						String checkPropertyValue = params.get("checkproperty_value").toString();

						WebElement we = isElementExist(wd, checkproperty, checkPropertyValue);
						if (null != we) {
							luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum()
									+ "步，在当前页面中找到预期结果中对象。当前步骤执行成功！");
							caselog.caseLogDetail(taskid, testcase.getSign(), "在当前页面中找到预期结果中对象。当前步骤执行成功！",
									"info", String.valueOf(step.getStepnum()),"");
							continue;
						} else {
							casenote = "第" + step.getStepnum() + "步，没有在当前页面中找到预期结果中对象。执行失败！";
							setresult = 1;
							java.text.DateFormat timeformat = new java.text.SimpleDateFormat("MMdd-hhmmss");
							imagname = timeformat.format(new Date());
							BaseWebDrive.webScreenShot(wd,imagname);
							luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum()
									+ "步，没有在当前页面中找到预期结果中对象。当前步骤执行失败！");
							caselog.caseLogDetail(taskid, testcase.getSign(), "在当前页面中没有找到预期结果中对象。当前步骤执行失败！"
									+ "checkproperty【"+checkproperty+"】  checkproperty_value【"+checkPropertyValue+"】","error", String.valueOf(step.getStepnum()),imagname);
							break;
						}

					}else{
						// 模糊匹配预期结果模式
						if (expectedResults.length()>2 && expectedResults.substring(0, 2).indexOf("%=")>-1) {
							if(result.indexOf(expectedResults.substring(2))>-1){
								luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum()
								+ "步，模糊匹配预期结果成功！执行结果："+result);
						        caselog.caseLogDetail(taskid, testcase.getSign(), "步骤模糊匹配预期结果成功！",
								"info", String.valueOf(step.getStepnum()),"");
						        continue;
							}else{
								casenote = "第" + step.getStepnum() + "步，模糊匹配预期结果失败！";
								setresult = 1;
								java.text.DateFormat timeformat = new java.text.SimpleDateFormat("MMdd-hhmmss");
								imagname = timeformat.format(new Date());
								BaseWebDrive.webScreenShot(wd,imagname);
								luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum()
								+ "步，模糊匹配预期结果失败！执行结果："+result);
						        caselog.caseLogDetail(taskid, testcase.getSign(), "步骤模糊匹配预期结果失败！执行结果："+result,
								"error", String.valueOf(step.getStepnum()),imagname);
								break;
							}
						}else if(expectedResults.equals(result)) {    // 直接匹配预期结果模式
							luckyclient.publicclass.LogUtil.APP.info("用例：" + testcase.getSign() + " 第" + step.getStepnum()
							+ "步，直接匹配预期结果成功！执行结果："+result);
					        caselog.caseLogDetail(taskid, testcase.getSign(), "步骤直接匹配预期结果成功！",
							"info", String.valueOf(step.getStepnum()),"");
					        continue;
						} else {
							casenote = "第" + step.getStepnum() + "步，直接匹配预期结果失败！";
							setresult = 1;
							java.text.DateFormat timeformat = new java.text.SimpleDateFormat("MMdd-hhmmss");
							imagname = timeformat.format(new Date());
							BaseWebDrive.webScreenShot(wd,imagname);
							luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum()
							+ "步，直接匹配预期结果失败！执行结果："+result);
					        caselog.caseLogDetail(taskid, testcase.getSign(), "步骤直接匹配预期结果失败！执行结果："+result,
							"error", String.valueOf(step.getStepnum()),imagname);
							break;
						}
					}
				}

			} else {
				casenote = result;
				setresult = 2;
				java.text.DateFormat timeformat = new java.text.SimpleDateFormat("MMdd-hhmmss");
				imagname = timeformat.format(new Date());
				BaseWebDrive.webScreenShot(wd,imagname);
				luckyclient.publicclass.LogUtil.APP.error("用例：" + testcase.getSign() + " 第" + step.getStepnum()	+ "步，"+result);
		        caselog.caseLogDetail(taskid, testcase.getSign(), "当前步骤在执行过程中解析|定位元素|操作对象失败！"+result,
				"error", String.valueOf(step.getStepnum()),imagname);
				break;
			}

		}

		variable.clear();
		caselog.updateCaseDetail(taskid, testcase.getSign(), setresult);
		if(setresult==0){
			luckyclient.publicclass.LogUtil.APP.info("用例【"+testcase.getSign()+"】全部步骤执行结果成功...");
	        caselog.caseLogDetail(taskid, testcase.getSign(), "用例全部步骤执行结果成功","info", "ending","");
		}else{
			luckyclient.publicclass.LogUtil.APP.error("用例【"+testcase.getSign()+"】步骤执行过程中失败或是锁定...请查看具体原因！"+casenote);
	        caselog.caseLogDetail(taskid, testcase.getSign(), "用例执行过程中失败或是锁定"+casenote,"error", "ending","");
		}
		//LogOperation.UpdateTastdetail(taskid, 0);
	}

	private static String runStep(Map<String, String> params, WebDriver wd,String taskid,String casenum,int stepno,LogOperation caselog) {
		String result = "";
		String property;
		String propertyValue;
		String operation;
		String operationValue;

		try {
			property = params.get("property");
			propertyValue = params.get("property_value");
			operation = params.get("operation");
			operationValue = params.get("operation_value");

			// 处理值传递
			property = ChangString.changparams(property, variable,"定位方式");
			propertyValue=ChangString.changparams(propertyValue, variable,"定位路径");
			operation=ChangString.changparams(operation, variable,"操作");
			operationValue=ChangString.changparams(operationValue, variable,"操作参数");
			
			luckyclient.publicclass.LogUtil.APP.info("二次解析用例过程完成，等待进行对象操作......");
			caselog.caseLogDetail(taskid, casenum, "对象操作:"+operation+"; 操作值:"+operationValue,"info", String.valueOf(stepno),"");

		} catch (Exception e) {
			e.printStackTrace();
			luckyclient.publicclass.LogUtil.APP.error("二次解析用例过程抛出异常！---"+e.getMessage());
			return "解析用例失败!";
		}

		try {		
			//调用接口用例
			if(null != operation&&null != operationValue&&"runcase".equals(operation)){
				String[] temp=operationValue.split(",",-1);
				String ex = TestCaseExecution.oneCaseExecuteForWebDriver(temp[0],taskid,caselog);
				if(ex.indexOf("CallCase调用出错！")<=-1&&ex.indexOf("解析出错啦！")<=-1&&ex.indexOf("匹配失败")<=-1){
					return ex;
				}else{
					return "调用接口用例过程失败";
				}
			}
			
			WebElement we = null;

			if (null != property && null != propertyValue) { // 页面元素层
				we = isElementExist(wd, property, propertyValue);
				// 判断此元素是否存在
				if (null==we) {
					luckyclient.publicclass.LogUtil.APP.error("定位对象失败，isElementExist为null!");
					return "isElementExist定位元素过程失败！";
				}

				if (operation.indexOf("select") > -1) {
					result = EncapsulateOperation.selectOperation(we, operation, operationValue);
				} else if (operation.indexOf("get") > -1){
					result = EncapsulateOperation.getOperation(wd, we, operation);
				} else if (operation.indexOf("mouse") > -1){
					result = EncapsulateOperation.actionWeOperation(wd, we, operation, operationValue, property, propertyValue);
				} else {
					result = EncapsulateOperation.objectOperation(wd, we, operation, operationValue, property, propertyValue);
				}
			} else if (null==property && null != operation) { // Driver层操作				
				// 处理弹出框事件
				if (operation.indexOf("alert") > -1){
					result = EncapsulateOperation.alertOperation(wd, operation);
				}else if(operation.indexOf("mouse") > -1){
					result = EncapsulateOperation.actionOperation(wd, operation, operationValue);
				}else{
					result = EncapsulateOperation.driverOperation(wd, operation, operationValue);
				} 				
			}else{
				luckyclient.publicclass.LogUtil.APP.error("元素操作过程失败！");
				result =  "元素操作过程失败！";
			}
		} catch (Exception e) {
			luckyclient.publicclass.LogUtil.APP.error("元素定位过程或是操作过程失败或异常！"+e.getMessage());
			return "元素定位过程或是操作过程失败或异常！" + e.getMessage();
		}
		caselog.caseLogDetail(taskid, casenum, result,"info", String.valueOf(stepno),"");
		
		if(result.indexOf("获取到的值是【")>-1&&result.indexOf("】")>-1){
			result = result.substring(7, result.length()-1);
		}
		return result;

	}

	public static WebElement isElementExist(WebDriver wd, String property, String propertyValue) {
		try {
			WebElement we = null;

			// 处理WebElement对象定位
			switch (property) {
			case "id":
				we = wd.findElement(By.id(propertyValue));
				break;
			case "name":
				we = wd.findElement(By.name(propertyValue));
				break;
			case "xpath":
				we = wd.findElement(By.xpath(propertyValue));
				break;
			case "linktext":
				we = wd.findElement(By.linkText(propertyValue));
				break;
			case "tagname":
				we = wd.findElement(By.tagName(propertyValue));
				break;
			case "cssselector":
				we = wd.findElement(By.cssSelector(propertyValue));
				break;
			default:
				break;
			}

			return we;

		} catch (Exception e) {
			luckyclient.publicclass.LogUtil.APP.error("当前对象定位失败："+e.getMessage());
			return null;
		}
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}

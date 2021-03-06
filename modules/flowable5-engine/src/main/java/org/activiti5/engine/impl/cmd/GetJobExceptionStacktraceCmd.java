/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti5.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.engine.runtime.Job;
import org.activiti5.engine.ActivitiIllegalArgumentException;
import org.activiti5.engine.ActivitiObjectNotFoundException;
import org.activiti5.engine.impl.interceptor.Command;
import org.activiti5.engine.impl.interceptor.CommandContext;
import org.activiti5.engine.impl.persistence.entity.JobEntity;


/**
 * @author Frederik Heremans
 */
public class GetJobExceptionStacktraceCmd implements Command<String>, Serializable{

  private static final long serialVersionUID = 1L;
  private String jobId;
    
  public GetJobExceptionStacktraceCmd(String jobId) {
    this.jobId = jobId;
  }


  public String execute(CommandContext commandContext) {
    if(jobId == null) {
      throw new ActivitiIllegalArgumentException("jobId is null");
    }
    
    JobEntity job = commandContext
      .getJobEntityManager()
      .findJobById(jobId);
    
    if(job == null) {
      throw new ActivitiObjectNotFoundException("No job found with id " + jobId, Job.class);
    }
    
    return job.getExceptionStacktrace();
  }

  
}

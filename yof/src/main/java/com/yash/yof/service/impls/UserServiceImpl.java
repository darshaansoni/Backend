package com.yash.yof.service.impls;

import com.yash.yof.Dto.UpdatePasswordDto;
import com.yash.yof.Dto.UserDto;
import com.yash.yof.Dto.UserRoleDto;
import com.yash.yof.constansts.AppConstants;
import com.yash.yof.constansts.UserAccountStatusTypes;
import com.yash.yof.constansts.UserRoleTypes;
import com.yash.yof.entity.User;
import com.yash.yof.repository.UserRepository;
import com.yash.yof.service.IService.IUserService;

import com.yash.yof.exceptions.ApplicationException;

import com.yash.yof.utils.EmailService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleServiceImpl userRoleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public String createNewUser(UserDto userDto) {

        User user = null;
        String message = null;
        if (ObjectUtils.isNotEmpty(userDto)) {

            user = this
                    .userRepository
                    .getUserByEmail(userDto.getEmailAdd());

            if (ObjectUtils.isEmpty(user)) {
                //user do not exist, new user will be created
                if (StringUtils.equals(userDto.getPassword(), userDto.getConfirmPassword())) {

                    if (ObjectUtils.isNotEmpty(userDto.getEmpId())) {
                        UserRoleDto userRoleDto =
                                userRoleService
                                        .getUserRoleByRoleName(UserRoleTypes.ASSOCIATE.toString());

                        userDto.setUserRole(userRoleDto);

                        user = this
                                .modelMapper
                                .map(userDto, User.class);

                        user.setPassword(this.passwordEncoder.encode(userDto.getPassword()));
                        user.setAccountStatus(UserAccountStatusTypes.PENDING);

                        //reassigned with the new created data
                        user = this
                                .userRepository
                                .save(user);

                        if (ObjectUtils.isNotEmpty(user)){
                            String body = String.format("Hi %1$s, \n\nYour request has been sent to the admin for approval. " + "Once approved, you will receive an email with login credentials. \n\nBest regards, \nYOF Team", user.getFullName());

//                            this.emailService.sendEmail(user.getEmailAdd(),"Your Account has been created.",body);
                            message = AppConstants.NEW_USER_REGISTRATION_SUCCESS_MESSAGE;}
                        else
                            message = "YOF User creation failed";
                    } else
                        throw new ApplicationException("Employee Id is empty or null, please enter valid employee id");
                } else
                    throw new ApplicationException("Password did not matched, please try again");
            } else
                throw new ApplicationException("User already exists with this email address");
        } else
            throw new ApplicationException("Invalid user details");

        return message;
    }

    @Override
    public List<UserDto> getAllAssociates() {
        List<User> allAssiciates = this.userRepository.findAllUsersByRole(UserRoleTypes.ASSOCIATE.toString());
        if (!allAssiciates.isEmpty()) {
            return allAssiciates
                    .stream()
                    .map(yur -> this
                            .modelMapper
                            .map(yur, UserDto.class))
                    .collect(Collectors.toList());
        } else
            throw new ApplicationException("No Associates found !");
    }

    @Override
    public UserDto getAssociateById(long id) {
        User user = userRepository.getUserByEmpId(id);
        if (user != null)
            return modelMapper.map(user, UserDto.class);
        else
            throw new ApplicationException("No Associate Found!");
    }

    @Override
    public UserDto deleteAssociateById(Long empId) {
        User user = this.userRepository.getUserByEmpId(empId);
        Integer associateDeleted = userRepository.deleteUserByEmpId(empId);
        if (associateDeleted > 0)
            return modelMapper.map(user, UserDto.class);
        return null;
    }


    @Override
    public String updatePassword(Long empId, UpdatePasswordDto updatePasswordDto) {
        User user = userRepository.getUserByEmpId(empId);
        String message ;

        if(!passwordEncoder.matches(updatePasswordDto.getOldPassword(),user.getPassword())){
            throw new ApplicationException("Old Password is incorrect!!!");
        }
        if(updatePasswordDto.getNewPassword().equals(updatePasswordDto.getOldPassword())){
            throw new ApplicationException("Old Password and New password cannot be same!!");
        }
        if (!updatePasswordDto.getNewPassword().equals(updatePasswordDto.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match!!!");
        }

        user.setPassword(passwordEncoder.encode(updatePasswordDto.getNewPassword()));
        userRepository.save(user);
        message = AppConstants.PASSWORD_UPDATE_SUCCESS_MESSAGE;
        return message;
    }


}


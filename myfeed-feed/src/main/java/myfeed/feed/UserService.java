package myfeed.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.hystrix.contrib.javanica.command.ObservableResult;
import myfeed.core.TraversonFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import rx.Observable;

/**
 * @author Spencer Gibb
 */
@Service
public class UserService {
	private static final ParameterizedTypeReference<PagedResources<Resource<User>>> TYPE_PAGED_USERS = new ParameterizedTypeReference<PagedResources<Resource<User>>>() {};

	private static final ParameterizedTypeReference<Resources<Resource<User>>> TYPE_USERS = new ParameterizedTypeReference<Resources<Resource<User>>>() {};

	@Autowired
	private RestTemplate rest;

	@Autowired
	private TraversonFactory traverson;

	@HystrixCommand(fallbackMethod = "defaultId")
	public Observable<String> findId(String username) {
		return new ObservableResult<String>() {
			@Override
			public String invoke() {
				ResponseEntity<User> user = rest.getForEntity("http://myfeed-user/@{username}", User.class, username);
				if (user.getStatusCode().equals(HttpStatus.OK)) {
					return user.getBody().getUserId();
				}
				return null;
			}
		};
	}

	public String defaultId(String username) {
		return null;
	}

	@HystrixCommand(fallbackMethod = "defaultUsers")
	public List<Resource<User>> getUsers() {
		PagedResources<Resource<User>> users = traverson.create("myfeed-user").follow("users").toObject(TYPE_PAGED_USERS);
		return new ArrayList<>(users.getContent());
	}

	public List<Resource<User>> defaultUsers() {
		return Collections.emptyList();
	}

	public List<Resource<User>> getFollowing(String userid) {
		Resources<Resource<User>> users = traverson.create("myfeed-user")
				.follow("users", "search", "findByFollowing")
				.withTemplateParameters(Collections.singletonMap("userId", userid))
				.toObject(TYPE_USERS);

		return new ArrayList<>(users.getContent());
	}
}
